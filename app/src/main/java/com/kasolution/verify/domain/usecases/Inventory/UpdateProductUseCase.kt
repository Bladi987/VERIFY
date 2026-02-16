package com.kasolution.verify.domain.usecases.Inventory

import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.data.repository.InventoryRepository


class UpdateProductUseCase(val repository: InventoryRepository) {
    operator fun invoke(product: Product, requestId: String) {
        repository.updateProduct(product,requestId)
    }
}