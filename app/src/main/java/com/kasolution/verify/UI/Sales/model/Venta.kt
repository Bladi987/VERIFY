package com.kasolution.verify.UI.Sales.model

data class Venta(
    val idCliente: Int,
    val idEmpleado: Int,
    val total: Double,
    val metodoPago: String, // 'EFECTIVO', 'TARJETA', etc.
    val estado: String = "PAGADO"
)