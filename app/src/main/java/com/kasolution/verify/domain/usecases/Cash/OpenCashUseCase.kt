package com.kasolution.verify.domain.usecases.Cash

import com.kasolution.verify.data.repository.CashRepository

class OpenCashUseCase(val repository: CashRepository) {
    operator fun invoke(idEmpleado: Int, montoApertura: Double, requestId: String) {
        repository.openCash(idEmpleado, montoApertura, requestId)
    }
}