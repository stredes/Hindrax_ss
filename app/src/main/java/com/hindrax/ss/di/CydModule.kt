package com.hindrax.ss.di

import com.hindrax.ss.data.cyd.CydRepositoryImpl
import com.hindrax.ss.domain.cyd.CydRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CydModule {

    @Binds
    @Singleton
    abstract fun bindCydRepository(
        cydRepositoryImpl: CydRepositoryImpl
    ): CydRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .build()
        }
    }
}
