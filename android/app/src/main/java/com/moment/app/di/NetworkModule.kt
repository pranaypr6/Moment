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

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
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
}
