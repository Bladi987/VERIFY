package com.kasolution.verify.domain.purchase

import com.kasolution.verify.data.repository.PurchaseRepository

class DeletePurchaseUseCase(private val repository: PurchaseRepository) {
    operator fun invoke(idCompra: Int, requestId: String) {
        // Al igual que en ventas, pasamos el ID y el identificador de la petición
        repository.deletePurchase(idCompra, requestId)
    }
}