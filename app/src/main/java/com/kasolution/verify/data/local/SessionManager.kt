package com.kasolution.verify.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("VerifyPrefs", Context.MODE_PRIVATE)

    // --- SESIÓN DE USUARIO (LOGIN) ---
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
    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getUserRole(): String = prefs.getString("user_role", "") ?: ""

    fun saveActiveCashSessionId(idSesion: Int) {
        prefs.edit().putInt("active_cash_session_id", idSesion).apply()
    }

    fun getActiveCashSessionId(): Int = prefs.getInt("active_cash_session_id", 0)

    fun clearCashSession() {
        prefs.edit().remove("active_cash_session_id").apply()
    }

    // --- CIERRE TOTAL ---
    fun logout() {
        prefs.edit().clear().apply()
    }
}