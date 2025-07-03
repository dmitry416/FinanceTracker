package com.example.finance_tracker

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_THEME = "theme_mode"
    }

    fun saveUserId(userId: Int) {
        editor.putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    fun clearSession() {
        editor.clear().apply()
    }

    fun saveThemeMode(mode: Int) {
        editor.putInt(KEY_THEME, mode).apply()
    }

    fun getThemeMode(): Int {
        return sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}