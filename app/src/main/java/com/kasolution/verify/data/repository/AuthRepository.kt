package com.kasolution.verify.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kasolution.verify.data.remote.dto.LoginResponseDto
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.domain.access.model.LoginResult
import com.google.gson.JsonParser

class AuthRepository(
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
){
    private val TAG = "AuthRepository"
    private val gson = Gson()
    private var isAuthenticating: Boolean = false

    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> get() = _loginResult

    init {
        registerObserver()
        setupErrorHandling()
    }

    private fun setupErrorHandling() {
        socketManager.onConnectionError = { errorMsg ->
            if (isAuthenticating) {
                _loginResult.postValue(LoginResult.Error("Error de red: $errorMsg"))
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
                    val responseDto = gson.fromJson(text, LoginResponseDto::class.java)

                    if (responseDto.status == "success") {
                        val data = responseDto.data
                        // Verificamos datos del DTO antes de guardarlos en sesión
                        if (data != null) {
                            sessionManager.saveSession(
                                data.id,
                                data.nombre ?: "Usuario",
                                data.rol ?: "Sin Rol"
                            )
                        }
                    }

                    // Mapeo DTO -> Domain para la UI
                    _loginResult.postValue(responseDto.toDomain())
                    isAuthenticating = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo Auth: ${e.message}")
                _loginResult.postValue(LoginResult.Error("Error al procesar respuesta del servidor"))
                isAuthenticating = false
            }
        }
    }

    fun login(usuario: String, pass: String) {
        if (isAuthenticating) return
        isAuthenticating = true
        _loginResult.value = null

        // Aprovechamos que sendAction ahora acepta Map<String, Any> y escapa caracteres
        val params = mapOf(
            "usuario" to usuario,
            "password" to pass
        )
        socketManager.sendAction("AUTH_LOGIN", params)
    }

    fun onSocketReconnected() {
        // Al reconectar, simplemente refrescamos la suscripción
        registerObserver()
    }

    // Este método es vital llamarlo desde el onCleared del ViewModel
    fun clear() {
        Log.d(TAG, "Dando de baja observador de Auth")
        socketManager.removeObserver(TAG)
        isAuthenticating = false
        _loginResult.postValue(null)
    }

    fun resetLoginState() {
        _loginResult.postValue(null)
        isAuthenticating = false
    }
}