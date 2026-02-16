package com.kasolution.verify.domain.usecases.Categories

import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.data.repository.CategoriesRepository


class SaveCategoryUseCase(val repository: CategoriesRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(category: Category, requestId: String) {
        repository.saveCategory(category,requestId)
    }
}