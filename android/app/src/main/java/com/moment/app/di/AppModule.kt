package com.moment.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.moment.app.data.local.MomentDao
import com.moment.app.data.local.MomentDatabase
import com.moment.app.data.remote.AuthApi
import com.moment.app.data.remote.MomentApi
import com.moment.app.data.repository.AuthRepositoryImpl
import com.moment.app.data.repository.MomentRepositoryImpl
import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.MomentRepository
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
    fun provideAuthRepository(api: AuthApi, prefs: SharedPreferences): AuthRepository {
        return AuthRepositoryImpl(api, prefs)
    }

    @Provides
    @Singleton
    fun provideRelationshipRepository(@ApplicationContext context: Context, api: com.moment.app.data.remote.RelationshipApi, prefs: SharedPreferences): com.moment.app.domain.repository.RelationshipRepository {
        return com.moment.app.data.repository.RelationshipRepositoryImpl(context, api, prefs)
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
    fun provideDeviceRepository(api: com.moment.app.data.remote.DeviceApi): com.moment.app.domain.repository.DeviceRepository {
        return com.moment.app.data.repository.DeviceRepositoryImpl(api)
    }
}
