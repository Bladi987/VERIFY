package com.kasolution.verify.domain.usecases.Cash

import com.kasolution.verify.data.repository.CashRepository

class GetCashHistoryUseCase(val repository: CashRepository) {
    operator fun invoke(idSesion: Int,  requestId: String) {
        repository.getCashHistory(idSesion, requestId)
    }
}