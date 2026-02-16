package com.kasolution.verify.domain.usecases.Categories

import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.data.repository.CategoriesRepository

class UpdateCategoryUseCase(val repository: CategoriesRepository) {
    operator fun invoke(category: Category, requestId: String) {
        repository.updateCategory(category,requestId)
    }
}