package com.kasolution.verify.UI.Sales.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase

class SalesViewModelFactory(
    private val getProductsUseCase: GetProductsUseCase,
    private val getClientsUseCase: GetClientsUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(
                getProductsUseCase,
                getClientsUseCase,
                socketManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}