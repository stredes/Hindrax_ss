package com.hindrax.ss.data.remote

import android.content.Context
import com.hindrax.ss.BuildConfig
import com.hindrax.ss.domain.sync.ApiHindraxBootstrapState
import com.hindrax.ss.domain.sync.ApiHindraxEndpoint
import com.hindrax.ss.domain.sync.ApiHindraxReleaseDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ApiHindraxConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val token: String
) {
    val isReady: Boolean
        get() = enabled && baseUrl.isNotBlank() && token.isNotBlank() && ApiHindraxEndpoint.isValid(baseUrl)
}

@Singleton
class ApiHindraxConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun load(): ApiHindraxConfig {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        val releaseDefaults = ApiHindraxReleaseDefaults(
            enabled = BuildConfig.API_HINDRAX_DEFAULT_ENABLED,
            baseUrl = BuildConfig.API_HINDRAX_DEFAULT_BASE_URL,
            token = BuildConfig.API_HINDRAX_DEFAULT_TOKEN
        )
        return ApiHindraxConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, releaseDefaults.isReady),
            baseUrl = ApiHindraxEndpoint.normalizeBaseUrl(
                prefs.getString(KEY_BASE_URL, releaseDefaults.normalizedBaseUrl)
                    ?: releaseDefaults.normalizedBaseUrl
            ),
            token = prefs.getString(KEY_TOKEN, releaseDefaults.token)
                ?: releaseDefaults.token
        )
    }

    fun shouldRunBootstrap(config: ApiHindraxConfig = load()): Boolean {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        return ApiHindraxBootstrapState.shouldRunBootstrap(
            isReady = config.isReady,
            baseUrl = config.baseUrl,
            completedBaseUrl = prefs.getString(KEY_BOOTSTRAP_COMPLETED_BASE_URL, "")
        )
    }

    fun markBootstrapComplete(baseUrl: String) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BOOTSTRAP_COMPLETED_BASE_URL, ApiHindraxEndpoint.normalizeBaseUrl(baseUrl))
            .putLong(KEY_BOOTSTRAP_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val KEY_ENABLED = "api_hindrax_enabled"
        const val KEY_BASE_URL = "api_hindrax_base_url"
        const val KEY_TOKEN = "api_hindrax_token"
        const val KEY_BOOTSTRAP_COMPLETED_BASE_URL = "api_hindrax_bootstrap_completed_base_url"
        const val KEY_BOOTSTRAP_COMPLETED_AT = "api_hindrax_bootstrap_completed_at"
    }
}
