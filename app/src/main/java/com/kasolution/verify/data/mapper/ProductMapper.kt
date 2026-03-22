package com.kasolution.verify.data.mapper
import com.kasolution.verify.data.remote.dto.ProductDto
import com.kasolution.verify.domain.Inventory.model.Product

fun ProductDto.toDomain() = Product(
    id = this.id ?: 0,
    codigo = this.codigo ?: "S/C",
    nombre = this.nombre ?: "Sin Nombre",
    // Ya no necesitamos toString() ni toIntOrNull() porque el DTO ya es Int?
    idCategoria = this.idCategoria ?: 0,
    nombreCategoria = this.nombreCategoria ?: "General",
    idProveedor = this.idProveedor ?: 0,
    nombreProveedor = this.nombreProveedor ?: "S/P",
    precioCompra = this.precioCompra ?: 0.0,
    precioVenta = this.precioVenta ?: 0.0,
    stock = this.stock ?: 0,
    unidadMedida = this.unidadMedida ?: "unidad",
    // Simplificamos la lógica del estado ya que el DTO captura el Int de la DB
    estado = this.estado ?: true
)

fun Product.toDto() = ProductDto(
    id = this.id,
    codigo = this.codigo,
    nombre = this.nombre,
    idCategoria = this.idCategoria,
    nombreCategoria = this.nombreCategoria,
    idProveedor = this.idProveedor,
    nombreProveedor = this.nombreProveedor,
    precioCompra = this.precioCompra,
    precioVenta = this.precioVenta,
    stock = this.stock,
    unidadMedida = this.unidadMedida,
    estado = this.estado ?: true
)