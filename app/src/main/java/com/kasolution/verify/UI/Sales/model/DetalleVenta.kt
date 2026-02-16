package com.kasolution.verify.UI.Sales.model

data class DetalleVenta(
    val idVenta: Long = 0,
    val idProducto: Int,
    val cantidad: Int,
    val precio: Double
)