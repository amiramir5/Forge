package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ApiKeyPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("motion_forge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_CLEAN_CONTROLS = "clean_controls"

        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var autoCleanControls: Boolean
        get() = prefs.getBoolean(KEY_CLEAN_CONTROLS, true)
        set(value) = prefs.edit().putBoolean(KEY_CLEAN_CONTROLS, value).apply()

    fun isApiKeyConfigured(): Boolean {
        val key = geminiApiKey
        return key.startsWith("AQ", ignoreCase = false) || key.startsWith("AIza", ignoreCase = false) || key.length > 20
    }
}
