package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.CategoryDto
import com.kasolution.verify.domain.Inventory.model.Category

fun CategoryDto.toDomain(): Category {
    return Category(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion ?: "",
        estado = this.estado
    )
}

// También es útil tener el inverso por si envías datos al servidor
fun Category.toDto(): CategoryDto {
    return CategoryDto(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        estado = this.estado
    )
}