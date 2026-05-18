package com.hindrax.ss.di

import android.content.Context
import com.hindrax.ss.data.db.*
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.data.repository.TargetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HindraxDatabase {
        return HindraxDatabase.getDatabase(context)
    }

    @Provides
    fun provideAuditSessionDao(database: HindraxDatabase): AuditSessionDao = database.auditSessionDao()

    @Provides
    fun provideAuditResultDao(database: HindraxDatabase): AuditResultDao = database.auditResultDao()

    @Provides
    fun provideAllowedTargetDao(database: HindraxDatabase): AllowedTargetDao = database.allowedTargetDao()

    @Provides
    fun provideToolTaskDao(database: HindraxDatabase): ToolTaskDao = database.toolTaskDao()

    @Provides
    fun provideTaskDao(database: HindraxDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideInventoryDao(database: HindraxDatabase): InventoryDao = database.inventoryDao()

    @Provides
    fun provideChatDao(database: HindraxDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideAuditRepository(
        sessionDao: AuditSessionDao,
        resultDao: AuditResultDao
    ): AuditRepository {
        return AuditRepository(sessionDao, resultDao)
    }

    @Provides
    @Singleton
    fun provideTargetRepository(allowedTargetDao: AllowedTargetDao): TargetRepository {
        return TargetRepository(allowedTargetDao)
    }
}
