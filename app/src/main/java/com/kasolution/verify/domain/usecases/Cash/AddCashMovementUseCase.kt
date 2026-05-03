package com.kasolution.verify.domain.usecases.Cash

import com.kasolution.verify.data.repository.CashRepository

class AddCashMovementUseCase(val repository: CashRepository) {
    operator fun invoke(idSesion: Int, tipo: String, monto: Double, motivo: String, requestId: String) {
        repository.addManualMovement(idSesion, tipo, monto, motivo, requestId)
    }
}