package com.kasolution.verify.domain.usecases.Categories

import com.kasolution.verify.data.repository.CategoriesRepository

class DeleteCategoryUseCase(val repository: CategoriesRepository) {
    operator fun invoke(id: Int, requestId: String) {
        repository.deleteCategory(id,requestId)
    }
}