package com.kasolution.verify.domain.usecases.Inventory

import com.kasolution.verify.data.repository.InventoryRepository

class DeleteProductUseCase(val repository: InventoryRepository) {
    operator fun invoke(id: Int, requestId: String) {
        repository.deleteProduct(id,requestId)
    }
}