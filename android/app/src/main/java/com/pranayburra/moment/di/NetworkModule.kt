package com.pranayburra.moment.di

import com.pranayburra.moment.data.remote.AuthApi
import com.pranayburra.moment.data.remote.MomentApi
import com.pranayburra.moment.data.remote.RelationshipApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton
import dagger.Lazy

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("AuthClient")
    fun provideAuthOkHttpClient(prefs: android.content.SharedPreferences): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        
        val errorInterceptor = networkErrorInterceptor()

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = prefs.getString("session_token", null)
            
            val requestBuilder = original.newBuilder()
            // Required to bypass ngrok's free tier browser warning page for API clients
            requestBuilder.header("ngrok-skip-browser-warning", "true")
            
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        }

        val authenticator = okhttp3.Authenticator { route, response ->
            val failedAuthHeader = response.request.header("Authorization") ?: return@Authenticator null

            // Give up after a couple of attempts rather than looping forever if the server
            // keeps rejecting whatever token we hand it back.
            var priorCount = 0
            var priorResponse = response.priorResponse
            while (priorResponse != null) {
                priorCount++
                priorResponse = priorResponse.priorResponse
            }
            if (priorCount >= 2) return@Authenticator null

            // Synchronized because several requests can hit a 401 for the same expired
            // access token at once (e.g. the handful of API calls MainViewModel fires off
            // together when the app resumes). Without this lock, each one would race to call
            // the refresh endpoint independently with the SAME (soon-to-be-invalidated)
            // refresh token. If refresh tokens are single-use/rotated server-side, the first
            // racing call succeeds and rotates it, but the second then gets legitimately
            // rejected by the server for using an already-consumed refresh token - which used
            // to wipe out the session that the first call had just successfully restored,
            // forcing a real logout for no real reason.
            synchronized(this@NetworkModule) {
                val currentToken = prefs.getString("session_token", null)
                val failedToken = failedAuthHeader.removePrefix("Bearer ")

                // Another thread already refreshed the token while we were waiting for the
                // lock - just retry with it instead of racing a second refresh call.
                if (currentToken != null && currentToken != failedToken) {
                    return@synchronized response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = prefs.getString("refresh_token", null) ?: return@synchronized null

                // Avoid dependency cycles by using a manual Retrofit builder just for auth
                val authRetrofit = Retrofit.Builder()
                    .baseUrl(com.pranayburra.moment.BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val authApi = authRetrofit.create(AuthApi::class.java)

                try {
                    val refreshCall = authApi.refreshTokenSync(com.pranayburra.moment.data.remote.RefreshTokenRequest(refreshToken))
                    val refreshResponse = refreshCall.execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!
                        prefs.edit()
                            .putString("session_token", newTokens.token)
                            .putString("refresh_token", newTokens.refreshToken)
                            .apply()

                        return@synchronized response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.token}")
                            .build()
                    } else if (refreshResponse.code() == 401 || refreshResponse.code() == 403) {
                        // The server explicitly rejected the refresh token itself (invalid/revoked) -
                        // this is the one case where the user genuinely needs to sign in again.
                        prefs.edit().clear().apply()
                        return@synchronized null
                    } else {
                        // Any other failure (5xx, malformed response, etc.) is a transient server
                        // problem, not proof the refresh token is invalid. Wiping the session here
                        // used to force a real logout every time the server had a blip - don't clear
                        // credentials, just fail this one request and let the app retry later while
                        // staying logged in.
                        com.pranayburra.moment.util.NetworkState.setOffline(true)
                        return@synchronized null
                    }
                } catch (e: Exception) {
                    // Network/IO failure while trying to refresh - also transient, not a logout trigger.
                    com.pranayburra.moment.util.NetworkState.setOffline(true)
                    return@synchronized null
                }
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(errorInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("CleanClient")
    fun provideCleanOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val errorInterceptor = networkErrorInterceptor()

        return OkHttpClient.Builder()
            .addInterceptor(errorInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Shared by both OkHttp clients. Previously a single failing request (IOException, or a
    // 502/503) immediately flipped the app into a full-screen "under maintenance" state, and
    // that flag stuck around until the user manually tapped "Try Again" - even if every other
    // concurrent request succeeded fine moments later. Cold app start fires several requests
    // at once (relationship fetch, pending-moment sync, profile, etc.), and a single transient
    // blip on just one of them (a slow DNS/TLS handshake, a brief cold-start delay on the
    // host) was enough to show the maintenance screen over an app that was actually working -
    // matching reports of "maintenance screen on open, then Try Again immediately works fine."
    // Now: retry once before declaring offline, and treat any successful response as proof
    // we're not offline, even if a different concurrent request just failed.
    private fun networkErrorInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()

        fun isTransientFailureCode(code: Int) = code == 502 || code == 503

        try {
            var response = chain.proceed(request)
            if (isTransientFailureCode(response.code)) {
                response.close()
                Thread.sleep(800)
                response = chain.proceed(request)
            }
            com.pranayburra.moment.util.NetworkState.setOffline(isTransientFailureCode(response.code))
            response
        } catch (e: java.io.IOException) {
            try {
                Thread.sleep(800)
                val retryResponse = chain.proceed(request)
                com.pranayburra.moment.util.NetworkState.setOffline(false)
                retryResponse
            } catch (retryException: java.io.IOException) {
                com.pranayburra.moment.util.NetworkState.setOffline(true)
                throw retryException
            }
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("AuthClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // 10.0.2.2 is the special alias to your host loopback interface (localhost) for the Android Emulator
            .baseUrl(com.pranayburra.moment.BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRelationshipApi(retrofit: Retrofit): RelationshipApi {
        return retrofit.create(RelationshipApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMomentApi(retrofit: Retrofit): MomentApi {
        return retrofit.create(MomentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeviceApi(retrofit: Retrofit): com.pranayburra.moment.data.remote.DeviceApi {
        return retrofit.create(com.pranayburra.moment.data.remote.DeviceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): com.pranayburra.moment.data.remote.ReportApi {
        return retrofit.create(com.pranayburra.moment.data.remote.ReportApi::class.java)
    }
}
