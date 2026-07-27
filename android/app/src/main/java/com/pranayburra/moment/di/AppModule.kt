package com.pranayburra.moment.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pranayburra.moment.data.local.MomentDao
import com.pranayburra.moment.data.local.MomentDatabase
import com.pranayburra.moment.data.remote.AuthApi
import com.pranayburra.moment.data.remote.MomentApi
import com.pranayburra.moment.data.repository.AuthRepositoryImpl
import com.pranayburra.moment.data.repository.MomentRepositoryImpl
import com.pranayburra.moment.domain.repository.AuthRepository
import com.pranayburra.moment.domain.repository.MomentRepository
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
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "moment_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Do not fallback to standard SharedPreferences to prevent saving sensitive tokens in plain text.
            throw SecurityException("Failed to create EncryptedSharedPreferences. Corrupted KeyStore.", e)
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(api: AuthApi, prefs: SharedPreferences, gson: com.google.gson.Gson, momentDatabase: MomentDatabase): AuthRepository {
        return AuthRepositoryImpl(api, prefs, gson, momentDatabase)
    }

    @Provides
    @Singleton
    fun provideRelationshipRepository(@ApplicationContext context: Context, api: com.pranayburra.moment.data.remote.RelationshipApi, prefs: SharedPreferences): com.pranayburra.moment.domain.repository.RelationshipRepository {
        return com.pranayburra.moment.data.repository.RelationshipRepositoryImpl(context, api, prefs)
    }

    @Provides
    @Singleton
    fun provideMomentDatabase(@ApplicationContext context: Context): MomentDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            MomentDatabase::class.java,
            "moment_db"
        )
        .fallbackToDestructiveMigration()
        .build()
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
        @Named("CleanClient") cleanClient: okhttp3.OkHttpClient,
        @ApplicationContext context: Context
    ): MomentRepository {
        return MomentRepositoryImpl(api, dao, cleanClient, context)
    }

    @Provides
    @Singleton
    fun provideDeviceRepository(api: com.pranayburra.moment.data.remote.DeviceApi): com.pranayburra.moment.domain.repository.DeviceRepository {
        return com.pranayburra.moment.data.repository.DeviceRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideReportRepository(api: com.pranayburra.moment.data.remote.ReportApi): com.pranayburra.moment.domain.repository.ReportRepository {
        return com.pranayburra.moment.data.repository.ReportRepositoryImpl(api)
    }
}
