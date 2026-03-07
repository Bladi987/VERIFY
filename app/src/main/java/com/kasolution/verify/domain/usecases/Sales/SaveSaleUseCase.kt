package com.kasolution.verify.domain.usecases.Sales

import com.kasolution.verify.data.repository.SalesRepository

class SaveSaleUseCase(val repository: SalesRepository) {
    operator fun invoke(
        idCliente: Int?,
        idEmpleado: Int,
        total: Double,
        metodoPago: String,
        idTipoComprobante: Int,
        detalles: List<Map<String, Any>>,
        requestId: String
    ) {
        repository.saveSale(idCliente, idEmpleado, total, metodoPago, idTipoComprobante, detalles, requestId)
    }
}