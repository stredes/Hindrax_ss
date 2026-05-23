package com.hindrax.ss.features.nfc

import android.content.Context
import com.hindrax.ss.domain.nfc.NfcHceEmulator

object NfcLabHceProfileStore {
    private const val PREFS = "nfc_lab_hce_profile"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PAYLOAD = "payload"

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun payload(context: Context): String {
        return NfcHceEmulator.normalizePayload(
            prefs(context).getString(KEY_PAYLOAD, NfcHceEmulator.DEFAULT_PAYLOAD).orEmpty()
        )
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setPayload(context: Context, payload: String) {
        prefs(context).edit()
            .putString(KEY_PAYLOAD, NfcHceEmulator.normalizePayload(payload))
            .apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
