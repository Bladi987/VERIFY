package com.kasolution.verify.domain.purchase

import com.kasolution.verify.data.repository.PurchaseRepository

class GetPurchaseDetailUseCase(private val repository: PurchaseRepository) {
    operator fun invoke(idCompra: Int) {
        repository.getPurchaseDetail(idCompra)
    }
}