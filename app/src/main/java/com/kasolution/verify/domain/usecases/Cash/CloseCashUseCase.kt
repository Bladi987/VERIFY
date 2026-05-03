package com.kasolution.verify.domain.usecases.Cash

import com.kasolution.verify.data.repository.CashRepository

class CloseCashUseCase(val repository: CashRepository) {
    operator fun invoke(idSesion: Int, montoCierre: Double, requestId: String) {
        repository.closeCash(idSesion, montoCierre, requestId)
    }
}