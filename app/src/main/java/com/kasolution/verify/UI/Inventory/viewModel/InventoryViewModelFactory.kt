package com.kasolution.verify.UI.Inventory.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Categories.GetCategoriesUseCase
import com.kasolution.verify.domain.usecases.Categories.SaveCategoryUseCase
import com.kasolution.verify.domain.usecases.Inventory.DeleteProductUseCase

import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Inventory.SaveProductUseCase
import com.kasolution.verify.domain.usecases.Inventory.UpdateProductUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase


class InventoryViewModelFactory(
    private val getProductsUseCase: GetProductsUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(
                getProductsUseCase,
                saveProductUseCase,
                updateProductUseCase,
                deleteProductUseCase,
                getSuppliersUseCase,
                getCategoriesUseCase,
                saveCategoryUseCase,
                socketManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}