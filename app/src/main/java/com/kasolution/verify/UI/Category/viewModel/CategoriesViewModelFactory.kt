package com.kasolution.verify.UI.Category.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Categories.DeleteCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.GetCategoriesUseCase
import com.kasolution.verify.domain.usecases.Categories.SaveCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.UpdateCategoryUseCase


class CategoriesViewModelFactory(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(getCategoriesUseCase, saveCategoryUseCase, updateCategoryUseCase, deleteCategoryUseCase, socketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}