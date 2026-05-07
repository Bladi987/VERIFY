package com.kasolution.verify.domain.usecases.reports

import com.kasolution.verify.data.repository.ReporteRepository

class GetMetodosPagoUseCase(val repository: ReporteRepository) {
    operator fun invoke(fInicio: String, fFin: String, requestId: String) =
        repository.getMetodosPago(fInicio, fFin, requestId)
}