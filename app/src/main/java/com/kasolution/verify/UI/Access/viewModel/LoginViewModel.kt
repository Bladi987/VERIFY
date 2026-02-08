package com.kasolution.verify.UI.Access.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.repository.AuthRepository

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    // Conectamos directamente al LiveData del repositorio Singleton
    val loginResult = repository.loginResult

    // Iniciamos en false para evitar que el ProgressBar aparezca por defecto
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    fun intentarLogin(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) return

        _isLoading.value = true
        repository.login(user, pass)

        // Timer de seguridad: si el servidor no responde, apagamos el cargando
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isLoading.value = false
        }, 10000)
    }
    fun resetState() {
        _isLoading.value = false
        repository.resetLoginState()
    }

    override fun onCleared() {
        super.onCleared()
    }
}