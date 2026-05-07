package com.kasolution.verify.domain.usecases.reports

import com.kasolution.verify.data.repository.ReporteRepository

class GetVentasUtilidadUseCase(val repository: ReporteRepository) {
    operator fun invoke(fechaInicio: String, fechaFin: String, requestId: String) {
        repository.getVentasUtilidad(fechaInicio, fechaFin, requestId)
    }
}