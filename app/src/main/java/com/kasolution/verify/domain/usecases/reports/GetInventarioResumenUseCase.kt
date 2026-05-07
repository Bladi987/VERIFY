package com.kasolution.verify.domain.usecases.reports

import com.kasolution.verify.data.repository.ReporteRepository

class GetInventarioResumenUseCase(val repository: ReporteRepository) {
    operator fun invoke(requestId: String) =
        repository.getInventarioResumen(requestId)
}