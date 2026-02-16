package com.kasolution.verify.UI.Suppliers.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Suppliers.DeleteSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase
import com.kasolution.verify.domain.usecases.Suppliers.SaveSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.UpdateSupplierUseCase


class SuppliersViewModelFactory(
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val saveSupplierUseCase: SaveSupplierUseCase,
    private val updateSupplierUseCase: UpdateSupplierUseCase,
    private val deleteSupplierUseCase: DeleteSupplierUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuppliersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuppliersViewModel(getSuppliersUseCase, saveSupplierUseCase, updateSupplierUseCase, deleteSupplierUseCase, socketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}