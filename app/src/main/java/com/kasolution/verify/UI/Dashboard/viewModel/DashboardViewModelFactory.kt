package com.kasolution.verify.UI.Dashboard.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.domain.usecases.Session.LogoutUseCase

class DashboardViewModelFactory(
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(sessionManager,logoutUseCase ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}