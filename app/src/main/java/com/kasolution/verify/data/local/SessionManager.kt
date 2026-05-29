package com.kasolution.verify.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("VerifyPrefs", Context.MODE_PRIVATE)

    // --- SESIÓN DE USUARIO (LOGIN) ---
    fun saveSession(
        idEmpleado: Int,
        nombre: String,
        rolSlug: String,
        permisos: List<String>,
        idSucursal: Int,
        sucursalNombre: String
    ) {
        prefs.edit().apply {
            putInt("user_id", idEmpleado)
            putString("user_name", nombre)
            putString("user_rol_slug", rolSlug)
            putString("user_permissions", permisos.joinToString(","))
            putInt("id_sucursal", idSucursal)
            putString("sucursal_nombre", sucursalNombre)
            putBoolean("is_logged_in", true)
            apply()
        }
    }
    fun saveUserModules(modulesJson: String) {
        prefs.edit().putString("user_modules_json", modulesJson).apply()
    }

    fun getUserModulesJson(): String = prefs.getString("user_modules_json", "[]") ?: "[]"
    fun isUserLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getUserRolSlug(): String = prefs.getString("user_rol_slug", "INVITADO") ?: "INVITADO"

    fun hasRole(roleSlug: String): Boolean = getUserRolSlug() == roleSlug
    fun hasPermission(permission: String): Boolean {
        val savedPermissions = prefs.getString("user_permissions", "") ?: ""
        val list = savedPermissions.split(",")
        return list.contains(permission)
    }

    fun getUserRolesForDisplay(): String {
        val slug = getUserRolSlug()
        return if (slug.isEmpty() || slug == "INVITADO") "Sin Rol" else slug
    }

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