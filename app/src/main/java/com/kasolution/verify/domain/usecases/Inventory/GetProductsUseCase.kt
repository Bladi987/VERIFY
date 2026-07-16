package com.kasolution.verify.domain.usecases.Inventory

import com.kasolution.verify.data.repository.InventoryRepository


class GetProductsUseCase(val repository: InventoryRepository) {
    operator fun invoke(idSucursal: Int, modo: String) {
        repository.getProducts(idSucursal, modo)
    }
}