package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.SaleDto
import com.kasolution.verify.domain.sales.model.Sale

fun SaleDto.toDomain() = Sale(
    id = when(val idVal = this.id) {
        is Number -> idVal.toInt()
        is String -> idVal.toIntOrNull() ?: 0
        else -> 0
    },
    idCliente = this.idCliente.toString().toIntOrNull() ?: 0,
    idEmpleado = this.idEmpleado.toString().toIntOrNull() ?: 0,
    total = this.total.toString().toDoubleOrNull() ?: 0.0,
    metodoPago = this.metodoPago ?: "EFECTIVO",
    estado = this.estado ?: "PAGADO"
)

fun Sale.toDto() = SaleDto(
    id = this.id,
    idCliente = this.idCliente,
    idEmpleado = this.idEmpleado,
    total = this.total,
    metodoPago = this.metodoPago,
    estado = this.estado
)