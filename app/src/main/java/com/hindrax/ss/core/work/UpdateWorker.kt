package com.hindrax.ss.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hindrax.ss.core.util.UpdateManager
import com.hindrax.ss.core.util.UpdateResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateManager: UpdateManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
        val currentVersion = packageInfo.versionName ?: "1.0.0"

        return try {
            when (updateManager.checkForUpdates(currentVersion)) {
                is UpdateResult.Available -> {
                    // La actualización está lista. El Dashboard mostrará el aviso.
                    Result.success()
                }
                is UpdateResult.NoUpdate -> Result.success()
                is UpdateResult.Error -> Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
