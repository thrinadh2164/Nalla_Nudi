package com.example.nallanudi

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class NallaNudiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("nalla_nudi_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("is_dark_mode", false)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}

// Author: E Thrinadh Chowdary
