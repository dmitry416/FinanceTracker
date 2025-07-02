package com.example.finance_tracker

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUserId(userId: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt("user_id", userId)
        editor.apply()
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt("user_id", -1)
    }

    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}