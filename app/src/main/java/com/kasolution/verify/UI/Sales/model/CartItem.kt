package com.kasolution.verify.UI.Sales.model

import com.kasolution.verify.UI.Inventory.model.Product

data class CartItem(
    val producto: Product, // Tu clase original
    var cantidad: Int = 1
) {
    // Calculamos el subtotal dinámicamente
    val subtotal: Double
        get() = producto.precioVenta * cantidad
}