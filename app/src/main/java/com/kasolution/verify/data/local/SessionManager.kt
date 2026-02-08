package com.kasolution.verify.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("VerifyPrefs", Context.MODE_PRIVATE)

    fun saveSession(id: Int, nombre: String, rol: String) {
        prefs.edit().apply {
            putInt("user_id", id)
            putString("user_name", nombre)
            putString("user_role", rol)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun isUserLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""

    fun getUserRole(): String = prefs.getString("user_role", "") ?: ""

    fun logout() {
        prefs.edit().clear().apply()
    }
}