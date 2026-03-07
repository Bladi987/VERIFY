package com.kasolution.verify.data.mapper
import com.kasolution.verify.data.remote.dto.ProductDto
import com.kasolution.verify.domain.Inventory.model.Product

fun ProductDto.toDomain() = Product(
    id = this.id,
    codigo = this.codigo ?: "S/C",
    nombre = this.nombre ?: "Sin Nombre",
    idCategoria = this.idCategoria?.toIntOrNull() ?: 0,
    nombreCategoria = this.nombreCategoria,
    idProveedor = this.idProveedor?.toIntOrNull() ?: 0,
    nombreProveedor = this.nombreProveedor,
    precioCompra = this.precioCompra?.toDoubleOrNull() ?: 0.0,
    precioVenta = this.precioVenta?.toDoubleOrNull() ?: 0.0,
    stock = this.stock?.toIntOrNull() ?: 0,
    unidadMedida = this.unidadMedida ?: "unidad",
    estado = when(this.estado) {
        is Boolean -> this.estado
        is Number -> this.estado.toInt() == 1
        is String -> this.estado == "1" || this.estado.lowercase() == "true"
        else -> false
    }
)

fun Product.toDto() = ProductDto(
    id = this.id,
    codigo = this.codigo,
    nombre = this.nombre,
    // Convertimos los números a String porque el DTO ahora los espera así
    idCategoria = this.idCategoria.toString(),
    nombreCategoria = this.nombreCategoria,
    idProveedor = this.idProveedor.toString(),
    nombreProveedor = this.nombreProveedor,
    precioCompra = this.precioCompra.toString(),
    precioVenta = this.precioVenta.toString(),
    stock = this.stock.toString(),
    unidadMedida = this.unidadMedida,
    estado = this.estado // El DTO acepta Any?, así que el Boolean pasará bien
)