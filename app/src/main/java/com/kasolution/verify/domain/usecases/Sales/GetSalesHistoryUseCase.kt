package com.kasolution.verify.domain.usecases.Sales

import com.kasolution.verify.data.repository.SalesRepository

class GetSalesHistoryUseCase(val repository: SalesRepository) {
    operator fun invoke() {
        repository.getSalesHistory()
    }
}