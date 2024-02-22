package com.example.AnonymousChat.active

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    fun saveLastClickTime(userId: String, time: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong("last_click_time_$userId", time)
        editor.apply()
    }

    fun getLastClickTime(userId: String): Long {
        return sharedPreferences.getLong("last_click_time_$userId", 0)
    }
}
