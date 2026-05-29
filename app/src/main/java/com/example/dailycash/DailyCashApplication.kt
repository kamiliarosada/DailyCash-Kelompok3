package com.example.dailycash

import android.app.Application
import com.example.dailycash.utils.PreferenceManager

class DailyCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val preferenceManager = PreferenceManager(this)
        preferenceManager.applyDarkMode(preferenceManager.isDarkMode())
    }
}
