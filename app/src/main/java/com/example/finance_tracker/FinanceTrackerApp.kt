package com.example.finance_tracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class FinanceTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
    }
}