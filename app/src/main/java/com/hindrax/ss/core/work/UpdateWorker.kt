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

        return when (updateManager.checkForUpdates(currentVersion)) {
            is UpdateResult.Available -> {
                // Aquí se podría disparar una notificación persistente
                // o iniciar la descarga si la configuración lo permite
                Result.success()
            }
            is UpdateResult.NoUpdate -> Result.success()
            is UpdateResult.Error -> Result.retry()
        }
    }
}
