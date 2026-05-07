package com.kasolution.verify.domain.usecases.reports

import com.kasolution.verify.data.repository.ReporteRepository

class GetCajaMovimientosUseCase(val repository: ReporteRepository) {
    operator fun invoke(fInicio: String, fFin: String, requestId: String) =
        repository.getCajaMovimientos(fInicio, fFin, requestId)
}