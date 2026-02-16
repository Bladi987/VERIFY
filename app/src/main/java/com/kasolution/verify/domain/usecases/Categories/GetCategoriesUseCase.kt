package com.kasolution.verify.domain.usecases.Categories

import com.kasolution.verify.data.repository.CategoriesRepository


class GetCategoriesUseCase(val repository: CategoriesRepository) {
    // Los casos de uso suelen tener una única función principal
    operator fun invoke() {
        repository.getCategories()
    }
}