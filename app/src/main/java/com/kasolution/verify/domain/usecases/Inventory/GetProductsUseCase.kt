package com.kasolution.verify.domain.usecases.Inventory

import com.kasolution.verify.data.repository.InventoryRepository


class GetProductsUseCase(val repository: InventoryRepository) {
    // Los casos de uso suelen tener una única función principal
    operator fun invoke() {
        repository.getProducts()
    }
}