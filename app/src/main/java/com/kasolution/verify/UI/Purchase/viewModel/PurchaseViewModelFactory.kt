package com.kasolution.verify.UI.Purchase.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.purchase.*
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase

class PurchaseViewModelFactory(
    private val sesionManager: SessionManager,
    private val savePurchaseUseCase: SavePurchaseUseCase,
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
    private val deletePurchaseUseCase: DeletePurchaseUseCase,
    private val getPurchaseDetailUseCase: GetPurchaseDetailUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PurchaseViewModel(
                sesionManager,
                savePurchaseUseCase,
                getPurchaseHistoryUseCase,
                deletePurchaseUseCase,
                getPurchaseDetailUseCase,
                getProductsUseCase,
                getSuppliersUseCase,
                socketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}