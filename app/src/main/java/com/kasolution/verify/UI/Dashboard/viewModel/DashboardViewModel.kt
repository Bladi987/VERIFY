package com.kasolution.verify.UI.Dashboard.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.domain.usecases.Session.LogoutUseCase

class DashboardViewModel(sessionManager: SessionManager, private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val userName: String = sessionManager.getUserName()
    val userRole: String = sessionManager.getUserRole()

    private val _logoutCompleted = MutableLiveData<Boolean>()
    val logoutCompleted: LiveData<Boolean> get() = _logoutCompleted

    fun logout() {
        logoutUseCase.execute()   // 🔌 cierra socket + limpia repos
        _logoutCompleted.postValue(true)
    }
}