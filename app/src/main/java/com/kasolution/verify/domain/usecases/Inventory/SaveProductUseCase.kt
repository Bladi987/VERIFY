package com.kasolution.verify.domain.usecases.Inventory

import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.data.repository.InventoryRepository

class SaveProductUseCase(val repository: InventoryRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(product: Product, requestId: String) {
        repository.saveProduct(product,requestId)
    }
}