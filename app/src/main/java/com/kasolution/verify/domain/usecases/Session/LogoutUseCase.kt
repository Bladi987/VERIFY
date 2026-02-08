package com.kasolution.verify.domain.usecases.Session

import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager

class LogoutUseCase(
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    private val onLogout: () -> Unit
) {
    fun execute() {
        // 1. Cerrar socket y cancelar callbacks
        socketManager.close()

        // 2. Limpiar repositorios
        onLogout()

        // 3. Limpiar sesión
        sessionManager.logout()
    }
}