package com.kasolution.verify.domain.purchase

import com.kasolution.verify.data.repository.PurchaseRepository

class SavePurchaseUseCase(val repository: PurchaseRepository) {
    operator fun invoke(
        idProveedor: Int,
        idEmpleado: Int,
        total: Double,
        detalles: List<Map<String, Any>>,
        requestId: String
    ) {
        repository.savePurchase(idProveedor, idEmpleado, total, detalles, requestId)
    }
}