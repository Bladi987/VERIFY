package com.kasolution.verify.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.kasolution.verify.domain.auth.model.AuthSession

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("VerifyPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_AUTH_SESSION_JSON = "auth_session_json"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ACTIVE_CASH_SESSION_ID = "active_cash_session_id"
    }
    fun saveSession(session: AuthSession) {
        val sessionJson = gson.toJson(session)
        prefs.edit().apply {
            putString(KEY_AUTH_SESSION_JSON, sessionJson)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getSession(): AuthSession? {
        val sessionJson = prefs.getString(KEY_AUTH_SESSION_JSON, null) ?: return null
        return try {
            gson.fromJson(sessionJson, AuthSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isUserLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    // --- 2. GETTERS ESPECÍFICOS RÁPIDOS (DELEGADOS) ---

    fun getUserId(): Int = getSession()?.id ?: -1

    fun getUserName(): String = getSession()?.nombre ?: "Usuario"

    fun getUserRolSlug(): String = getSession()?.rolSlug ?: "NONE"

    fun getSucursalNombre(): String = getSession()?.sucursalNombre ?: "Sede no asignada"
    fun getSucursalId(): Int = getSession()?.idSucursal ?: 0

    // --- 3. COMPROBACIONES DE AUTORIZACIÓN ---

    fun hasRole(roleSlug: String): Boolean = getUserRolSlug() == roleSlug

    fun hasPermission(permission: String): Boolean {
        return getSession()?.permisos?.contains(permission) == true
    }

    fun getUserRolesForDisplay(): String {
        val slug = getUserRolSlug()
        return if (slug.isEmpty() || slug == "NONE") "Sin Rol" else slug
    }

    // --- 4. CONTROL DE CAJA COMPLEMENTARIO ---

    fun saveActiveCashSessionId(idSesion: Int) {
        prefs.edit().putInt(KEY_ACTIVE_CASH_SESSION_ID, idSesion).apply()
    }

    fun getActiveCashSessionId(): Int = prefs.getInt(KEY_ACTIVE_CASH_SESSION_ID, 0)

    fun clearCashSession() {
        prefs.edit().remove(KEY_ACTIVE_CASH_SESSION_ID).apply()
    }

    // --- 5. CIERRE TOTAL LIMPIO ---
    fun logout() {
        prefs.edit().clear().apply()
    }
}