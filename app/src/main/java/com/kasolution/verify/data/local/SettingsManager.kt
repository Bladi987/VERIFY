package com.kasolution.verify.data.local

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_IP = "server_ip"
        const val KEY_PORT = "server_port"
        const val DEFAULT_IP = ""
        const val DEFAULT_PORT = ""
    }

    // Verifica si el usuario ya guardó una configuración manualmente
    fun isConfigured(): Boolean {
        return prefs.contains(KEY_IP) && prefs.contains(KEY_PORT)
    }

    fun saveConfig(ip: String, port: String) {
        prefs.edit().apply {
            putString(KEY_IP, ip)
            putString(KEY_PORT, port)
            apply()
        }
    }

    fun getServerUrl(): String {
        val ip = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        val port = prefs.getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        return "ws://$ip:$port"
    }
    fun getIp(): String = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
    fun getPort(): String = prefs.getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
}