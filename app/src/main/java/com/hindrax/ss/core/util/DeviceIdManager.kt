package com.hindrax.ss.core.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString("device_hash", null)
        if (id == null) {
            id = generateUniqueHash()
            prefs.edit().putString("device_hash", id).apply()
        }
        return id
    }

    fun getNickname(): String {
        return prefs.getString("device_nickname", null)
            ?: "Hindrax_${getDeviceId().takeLast(4)}"
    }

    fun setNickname(nickname: String) {
        val normalized = nickname.trim()
        prefs.edit().apply {
            if (normalized.isBlank()) {
                remove("device_nickname")
            } else {
                putString("device_nickname", normalized)
            }
        }.apply()
    }

    private fun generateUniqueHash(): String {
        // Generate a random UUID and take the first 8-12 chars or hash it
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return "HNDX-${uuid.take(8).uppercase()}"
    }
}
