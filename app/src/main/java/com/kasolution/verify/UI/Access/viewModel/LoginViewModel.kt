package com.kasolution.verify.UI.Access.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.repository.AuthRepository

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    // Ahora este LiveData emitirá Success(User) o Error(String)
    // El repositorio se encargará de transformar el DTO a este formato
    val loginResult = repository.loginResult

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun intentarLogin(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            // Podrías emitir un error local aquí si quisieras
            return
        }

        _isLoading.value = true
        repository.login(user, pass)

        // Nota: En una arquitectura sólida, el "isLoading" suele apagarse
        // cuando el repositorio emite un resultado, pero tu timer de seguridad es válido.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isLoading.value = false
        }, 10000)
    }

    fun resetState() {
        _isLoading.value = false
        repository.resetLoginState()
    }
}