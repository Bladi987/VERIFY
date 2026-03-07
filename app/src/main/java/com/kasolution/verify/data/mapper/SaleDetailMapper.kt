package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.SaleDetailDto
import com.kasolution.verify.domain.sales.model.SaleDetail

fun SaleDetailDto.toDomain() = SaleDetail(
    idVenta = this.idVenta.toString().toLongOrNull() ?: 0L,
    idProducto = this.idProducto.toString().toIntOrNull() ?: 0,
    cantidad = this.cantidad.toString().toIntOrNull() ?: 0,
    precio = this.precio.toString().toDoubleOrNull() ?: 0.0
)

fun SaleDetail.toDto() = SaleDetailDto(
    idVenta = this.idVenta,
    idProducto = this.idProducto,
    cantidad = this.cantidad,
    precio = this.precio
)