package com.kasolution.verify.UI.Sales.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Sales.DeleteSaleUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSaleDetailUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSalesHistoryUseCase
import com.kasolution.verify.domain.usecases.Sales.SaveSaleUseCase

class SalesViewModelFactory(
    private val sesionManager: SessionManager,
    private val saveSaleUseCase: SaveSaleUseCase,
    private val getSalesHistoryUseCase: GetSalesHistoryUseCase,
    private val deleteSaleUseCase: DeleteSaleUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getClientsUseCase: GetClientsUseCase,
    private val getSaleDetailUseCase: GetSaleDetailUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(
                sesionManager,
                saveSaleUseCase,
                getSalesHistoryUseCase,
                deleteSaleUseCase,
                getProductsUseCase,
                getClientsUseCase,
                getSaleDetailUseCase,
                socketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}