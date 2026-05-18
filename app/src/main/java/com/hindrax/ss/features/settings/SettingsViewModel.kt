package com.hindrax.ss.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeys: Map<String, String> = emptyMap(),
    val isDarkTheme: Boolean = true,
    val saveReportsAutomatically: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        _uiState.value = SettingsUiState(
            apiKeys = mapOf(
                "Shodan" to (prefs.getString("api_key_shodan", "") ?: ""),
                "Hunter.io" to (prefs.getString("api_key_hunter", "") ?: "")
            ),
            isDarkTheme = prefs.getBoolean("dark_theme", true),
            saveReportsAutomatically = prefs.getBoolean("auto_save", false)
        )
    }

    fun updateApiKey(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("api_key_${key.lowercase()}", value).apply()
        loadSettings(context)
    }

    fun toggleAutoSave(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("hindrax_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_save", enabled).apply()
        loadSettings(context)
    }
}
