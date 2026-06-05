package com.moment.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.moment.app.data.local.MomentDao
import com.moment.app.data.local.MomentDatabase
import com.moment.app.data.remote.AuthApi
import com.moment.app.data.remote.ConnectionApi
import com.moment.app.data.remote.MomentApi
import com.moment.app.data.remote.TimelineApi
import com.moment.app.data.repository.AuthRepositoryImpl
import com.moment.app.data.repository.ConnectionRepositoryImpl
import com.moment.app.data.repository.MomentRepositoryImpl
import com.moment.app.data.repository.TimelineRepositoryImpl
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.ConnectionRepository
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.domain.repository.TimelineRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "moment_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideAuthRepository(api: AuthApi, prefs: SharedPreferences): AuthRepository {
        return AuthRepositoryImpl(api, prefs)
    }

    @Provides
    @Singleton
    fun provideConnectionRepository(api: ConnectionApi): ConnectionRepository {
        return ConnectionRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideMomentDatabase(@ApplicationContext context: Context): MomentDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            MomentDatabase::class.java,
            "moment_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMomentDao(db: MomentDatabase): MomentDao {
        return db.momentDao()
    }

    @Provides
    @Singleton
    fun provideMomentRepository(
        api: MomentApi,
        dao: MomentDao,
        @Named("AuthClient") authClient: OkHttpClient,
        @Named("CleanClient") cleanClient: OkHttpClient
    ): MomentRepository {
        return MomentRepositoryImpl(api, dao, authClient, cleanClient)
    }

    @Provides
    @Singleton
    fun provideTimelineRepository(api: TimelineApi): TimelineRepository {
        return TimelineRepositoryImpl(api)
    }
}
