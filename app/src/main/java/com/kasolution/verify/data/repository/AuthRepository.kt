package com.kasolution.verify.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kasolution.verify.data.remote.dto.AuthResponseDto
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.domain.auth.model.AuthResult
import com.google.gson.JsonParser

class AuthRepository(
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
){
    private val TAG = "AuthRepository"
    private val gson = Gson()
    private var isAuthenticating: Boolean = false

    private val _authResult = MutableLiveData<AuthResult?>()
    val authResult: LiveData<AuthResult?> get() = _authResult

    init {
        registerObserver()
        setupErrorHandling()
    }

    private fun setupErrorHandling() {
        socketManager.onConnectionError = { errorMsg ->
            if (isAuthenticating) {
                _authResult.postValue(AuthResult.Error("Error de red: $errorMsg"))
                isAuthenticating = false
            }
        }
    }

    private fun registerObserver() {
        socketManager.addObserver(TAG) { text ->
            try {
                // Usamos JsonParser de GSON para una lectura más ligera del "action"
                val jsonObject = JsonParser.parseString(text).asJsonObject

                if (jsonObject.get("action")?.asString == "AUTH_LOGIN") {
                    val responseDto = gson.fromJson(text, AuthResponseDto::class.java)
                    val domainResult = responseDto.toDomain()
                    if (domainResult is AuthResult.Success) {
                        sessionManager.saveSession(domainResult.AuthSession)
                    }

                    // Mapeo DTO -> Domain para la UI
                    _authResult.postValue(domainResult)
                    isAuthenticating = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo Auth: ${e.message}")
                _authResult.postValue(AuthResult.Error("Error al procesar respuesta del servidor"))
                isAuthenticating = false
            }
        }
    }

    fun login(usuario: String, pass: String) {
        if (isAuthenticating) return
        isAuthenticating = true
        _authResult.value = null

        // Aprovechamos que sendAction ahora acepta Map<String, Any> y escapa caracteres
        val params = mapOf(
            "usuario" to usuario,
            "password" to pass
        )
        socketManager.sendAction("AUTH_LOGIN", params)
    }

    fun onSocketReconnected() {
        registerObserver()
    }

    fun clear() {
        Log.d(TAG, "Dando de baja observador de Auth")
        socketManager.removeObserver(TAG)
        isAuthenticating = false
        _authResult.postValue(null)
    }

    fun resetLoginState() {
        _authResult.postValue(null)
        isAuthenticating = false
    }
}