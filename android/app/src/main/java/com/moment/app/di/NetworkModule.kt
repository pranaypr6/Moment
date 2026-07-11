package com.moment.app.di

import com.moment.app.data.remote.AuthApi
import com.moment.app.data.remote.MomentApi
import com.moment.app.data.remote.RelationshipApi
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
            level = HttpLoggingInterceptor.Level.BODY
        }
        
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
            if (response.request.header("Authorization") == null) return@Authenticator null
            
            val refreshToken = prefs.getString("refresh_token", null) ?: return@Authenticator null
            
            // Avoid dependency cycles by using a manual Retrofit builder just for auth
            val authRetrofit = Retrofit.Builder()
                .baseUrl("https://bribe-education-regime.ngrok-free.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val authApi = authRetrofit.create(AuthApi::class.java)
            
            try {
                val refreshCall = authApi.refreshTokenSync(com.moment.app.data.remote.RefreshTokenRequest(refreshToken))
                val refreshResponse = refreshCall.execute()
                
                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newTokens = refreshResponse.body()!!
                    prefs.edit()
                        .putString("session_token", newTokens.token)
                        .putString("refresh_token", newTokens.refreshToken)
                        .apply()
                        
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.token}")
                        .build()
                } else {
                    // Logout user if refresh fails
                    prefs.edit().clear().apply()
                    return@Authenticator null
                }
            } catch (e: Exception) {
                return@Authenticator null
            }
        }

        return OkHttpClient.Builder()
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
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("AuthClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // 10.0.2.2 is the special alias to your host loopback interface (localhost) for the Android Emulator
            .baseUrl("https://bribe-education-regime.ngrok-free.dev/")
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
    fun provideDeviceApi(retrofit: Retrofit): com.moment.app.data.remote.DeviceApi {
        return retrofit.create(com.moment.app.data.remote.DeviceApi::class.java)
    }
}
