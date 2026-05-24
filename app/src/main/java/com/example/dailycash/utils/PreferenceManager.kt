package com.example.dailycash.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("DailyCashPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun setRememberMe(remember: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
    }

    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun setDarkMode(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isEnabled).apply()
        applyDarkMode(isEnabled)
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun applyDarkMode(isEnabled: Boolean) {
        if (isEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
