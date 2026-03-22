package com.kasolution.verify.UI.Sales.model

import com.kasolution.verify.domain.Inventory.model.Product

data class CartItem(
    val producto: Product,
    var cantidad: Int = 1
) {
    // Regla de negocio: Subtotal calculado
    val subtotal: Double
        get() = producto.precioVenta * cantidad

    // Regla de negocio: No podemos vender más de lo que hay en el objeto Product
    fun canIncrease(): Boolean = cantidad < producto.stock
}