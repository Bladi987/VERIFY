package com.kasolution.verify.domain.usecases.reports

import com.kasolution.verify.data.repository.ReporteRepository

class GetTopProductosUseCase(val repository: ReporteRepository) {
    operator fun invoke(fechaInicio: String, fechaFin: String, requestId: String) {
        repository.getTopProductos(fechaInicio, fechaFin, requestId)
    }
}