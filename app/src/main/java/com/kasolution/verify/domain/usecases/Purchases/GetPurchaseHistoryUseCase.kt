package com.kasolution.verify.domain.purchase

import com.kasolution.verify.data.repository.PurchaseRepository

class GetPurchaseHistoryUseCase(private val repository: PurchaseRepository) {
    operator fun invoke() {
        repository.getPurchaseHistory()
    }
}