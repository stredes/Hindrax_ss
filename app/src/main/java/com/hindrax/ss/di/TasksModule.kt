package com.hindrax.ss.di

import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.tasks.repository.TaskRepositoryImpl
import com.hindrax.ss.domain.tasks.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TasksModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        inventoryDao: InventoryDao,
        chatRepository: com.hindrax.ss.data.repository.ChatRepository
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao, inventoryDao, chatRepository)
    }
}
