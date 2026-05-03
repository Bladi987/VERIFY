package com.kasolution.verify.domain.usecases.Sales

import com.kasolution.verify.data.repository.SalesRepository

class SaveSaleUseCase(val repository: SalesRepository) {
    operator fun invoke(
        idSesion: Int,
        idCliente: Int?,
        idEmpleado: Int,
        total: Double,
        pagos: List<Map<String, Any>>,
        idTipoComprobante: Int,
        detalles: List<Map<String, Any>>,
        requestId: String
    ) {
        repository.saveSale(idSesion,idCliente, idEmpleado, total, pagos, idTipoComprobante, detalles, requestId)
    }
}