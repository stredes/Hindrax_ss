package com.hindrax.ss.core.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hindrax.ss.MainActivity
import com.hindrax.ss.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class HindraxNotificationCenter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val meshChannel = NotificationChannel(
            CHANNEL_MESH,
            "Hindrax Mesh",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pairing, sync and mesh activity notifications"
        }
        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "Hindrax Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Application update notifications"
        }
        manager.createNotificationChannels(listOf(meshChannel, updatesChannel))
    }

    fun notifyNodeDetected(displayName: String, detail: String) {
        notify(
            channelId = CHANNEL_MESH,
            notificationId = stableId("node:$displayName:$detail"),
            title = "NODE_VISIBLE: $displayName",
            message = detail
        )
    }

    fun notifyPairing(peerName: String, detail: String) {
        notify(
            channelId = CHANNEL_MESH,
            notificationId = stableId("pair:$peerName"),
            title = "PAIRING_READY: $peerName",
            message = detail
        )
    }

    fun notifySync(peerName: String, detail: String) {
        notify(
            channelId = CHANNEL_MESH,
            notificationId = stableId("sync:$peerName:$detail"),
            title = "SYNC_UPDATE: $peerName",
            message = detail
        )
    }

    fun notifyMessage(peerName: String, message: String) {
        notify(
            channelId = CHANNEL_MESH,
            notificationId = stableId("message:$peerName:${System.currentTimeMillis()}"),
            title = "MESSAGE_FROM: $peerName",
            message = message.take(120)
        )
    }

    private fun notify(channelId: String, notificationId: Int, title: String, message: String) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.hindrax_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun stableId(value: String): Int {
        val hash = value.hashCode()
        return if (hash == Int.MIN_VALUE) 1 else hash.absoluteValue
    }

    companion object {
        const val CHANNEL_MESH = "hindrax_mesh"
        const val CHANNEL_UPDATES = "hindrax_updates"
    }
}
