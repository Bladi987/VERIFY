package com.kasolution.verify.UI.Cash.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Cash.*

class CashViewModelFactory(
    private val sessionManager: SessionManager,
    private val openCashUseCase: OpenCashUseCase,
    private val closeCashUseCase: CloseCashUseCase,
    private val getCashStatusUseCase: GetCashStatusUseCase,
    private val addCashMovementUseCase: AddCashMovementUseCase,
    private val getCashHistoryUseCase: GetCashHistoryUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CashViewModel(
                sessionManager,
                openCashUseCase,
                closeCashUseCase,
                getCashStatusUseCase,
                addCashMovementUseCase,
                getCashHistoryUseCase,
                socketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}