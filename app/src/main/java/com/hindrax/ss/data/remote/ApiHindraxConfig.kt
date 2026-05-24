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

data class ApiHindraxStoredConfig(
    val hasEnabled: Boolean,
    val enabled: Boolean,
    val baseUrl: String?,
    val token: String?
)

object ApiHindraxConfigResolver {
    fun resolve(stored: ApiHindraxStoredConfig, releaseDefaults: ApiHindraxReleaseDefaults): ApiHindraxConfig {
        val storedToken = stored.token?.takeIf { it.isNotBlank() }
        val storedBaseUrl = stored.baseUrl?.takeIf { it.isNotBlank() }
        val shouldUseReleaseDefaults = releaseDefaults.isReady &&
            (!stored.hasEnabled || (!stored.enabled && storedToken == null))

        return ApiHindraxConfig(
            enabled = if (shouldUseReleaseDefaults) true else stored.enabled,
            baseUrl = ApiHindraxEndpoint.normalizeBaseUrl(
                if (shouldUseReleaseDefaults) releaseDefaults.normalizedBaseUrl else storedBaseUrl ?: releaseDefaults.normalizedBaseUrl
            ),
            token = if (shouldUseReleaseDefaults) releaseDefaults.token else storedToken ?: releaseDefaults.token
        )
    }
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
        val config = ApiHindraxConfigResolver.resolve(
            stored = ApiHindraxStoredConfig(
                hasEnabled = prefs.contains(KEY_ENABLED),
                enabled = prefs.getBoolean(KEY_ENABLED, releaseDefaults.isReady),
                baseUrl = prefs.getString(KEY_BASE_URL, null),
                token = prefs.getString(KEY_TOKEN, null)
            ),
            releaseDefaults = releaseDefaults
        )
        if (config.isReady && !prefs.getBoolean(KEY_ENABLED, false)) {
            prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_BASE_URL, config.baseUrl)
                .putString(KEY_TOKEN, config.token)
                .apply()
        }
        return config
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
