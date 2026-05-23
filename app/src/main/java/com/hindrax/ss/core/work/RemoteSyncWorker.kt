package com.hindrax.ss.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hindrax.ss.data.remote.ApiHindraxRemoteSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RemoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val remoteSyncRepository: ApiHindraxRemoteSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = remoteSyncRepository.syncAll()
            if (!result.enabled) {
                Result.success()
            } else {
                Result.success()
            }
        } catch (error: Exception) {
            Result.retry()
        }
    }
}
