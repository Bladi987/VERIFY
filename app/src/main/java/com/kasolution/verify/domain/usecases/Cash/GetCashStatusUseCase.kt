package com.kasolution.verify.domain.usecases.Cash

import com.kasolution.verify.data.repository.CashRepository

class GetCashStatusUseCase(val repository: CashRepository) {
    operator fun invoke(idEmpleado: Int) {
        repository.getCashStatus(idEmpleado)
    }
}