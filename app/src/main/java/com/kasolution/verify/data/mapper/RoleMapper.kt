package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.RoleDto
import com.kasolution.verify.domain.role.model.Role

fun RoleDto.toDomain(): Role {
    return Role(
        id = idRol,
        nombre = nombreRol,
        slug = slugRol,
        descripcion = descripcion,
        estado = estado
    )
}