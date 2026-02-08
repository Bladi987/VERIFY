package com.kasolution.verify.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kasolution.verify.UI.Access.model.LoginResponse
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import org.json.JSONObject

class AuthRepository(
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
){

    private val TAG = "AuthRepository"
    private val gson = Gson()
    private var isAuthenticating: Boolean = false

    private val _loginResult = MutableLiveData<LoginResponse?>()
    val loginResult: LiveData<LoginResponse?> get() = _loginResult

    init {
        Log.d(TAG, "INIT: Registrando observer de Auth")
        registerObserver()
        setupErrorHandling()
    }

    private fun setupErrorHandling() {
        socketManager.onConnectionError = { errorMsg ->
            if (isAuthenticating) {
                val errorResponse = LoginResponse(
                    action = "CONECTION_ERROR",
                    status = "error",
                    message = "Servidor desconectado, intente más tarde.",
                    data = null
                )
                _loginResult.postValue(errorResponse)
                isAuthenticating = false
            }
        }
    }

    private fun registerObserver() {
        // Al ser Singleton, nos aseguramos de no duplicar el observer en el SocketManager
        socketManager.addObserver(TAG) { text ->
            try {
                val json = JSONObject(text)
                if (json.optString("action") == "AUTH_LOGIN") {
                    val response = gson.fromJson(text, LoginResponse::class.java)

                    if (response.status == "success" && response.data != null) {
                        sessionManager.saveSession(
                            response.data.id,
                            response.data.nombre,
                            response.data.rol
                        )
                    }

                    _loginResult.postValue(response)
                    isAuthenticating = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo Auth: ${e.message}")
            }
        }
    }

    fun login(usuario: String, pass: String) {
        if (isAuthenticating) return

        isAuthenticating = true
        val params = mapOf(
            "usuario" to usuario,
            "password" to pass
        )
        socketManager.sendAction("AUTH_LOGIN", params)
    }

    fun onSocketReconnected() {
        socketManager.removeObserver(TAG)
        registerObserver()
    }

    fun clear() {
        Log.d(TAG, "Limpiando AuthRepository")
        isAuthenticating = false
        _loginResult.postValue(null)
        socketManager.removeObserver(TAG)
    }

    fun resetLoginState() {
        _loginResult.postValue(null)
        isAuthenticating = false
    }
}