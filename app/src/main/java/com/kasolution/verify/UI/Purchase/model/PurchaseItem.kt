package com.kasolution.verify.UI.Purchase.model

data class PurchaseItem(
    val idProducto: Int,
    val nombre: String,
    var cantidad: Int,
    var precioCompra: Double,
    val stockActual: Int
)