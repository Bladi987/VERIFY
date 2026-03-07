package com.kasolution.verify.domain.usecases.Sales

import com.kasolution.verify.data.repository.SalesRepository


class GetSaleDetailUseCase(private val repository: SalesRepository) {
    operator fun invoke(idVenta: Int) {
        repository.getSaleDetail(idVenta)
    }
}