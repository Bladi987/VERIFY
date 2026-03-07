package com.kasolution.verify.domain.usecases.Sales

import com.kasolution.verify.data.repository.SalesRepository

class DeleteSaleUseCase(val repository: SalesRepository) {
    operator fun invoke(idVenta: Int, requestId: String) {
        repository.deleteSale(idVenta, requestId)
    }
}