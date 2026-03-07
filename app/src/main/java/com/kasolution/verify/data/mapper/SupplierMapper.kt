package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.SupplierDto
import com.kasolution.verify.domain.supplier.model.Supplier

fun SupplierDto.toDomain(): Supplier {
    return Supplier(
        // Convertimos el ID de forma segura sea String o Int
        id = when(val idVal = this.id) {
            is Number -> idVal.toInt()
            is String -> idVal.toIntOrNull() ?: 0
            else -> 0
        },
        nombre = this.nombre ?: "Sin nombre",
        telefono = this.telefono ?: "",
        email = this.email ?: "",
        direccion = this.direccion ?: ""
    )
}

fun Supplier.toDto(): SupplierDto {
    return SupplierDto(
        id = this.id,
        nombre = this.nombre,
        telefono = this.telefono,
        email = this.email,
        direccion = this.direccion
    )
}