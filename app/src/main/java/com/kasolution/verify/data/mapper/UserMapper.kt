package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.UserDto
import com.kasolution.verify.domain.access.model.User

fun UserDto.toDomain(): User {
    return User(
        id = this.id,
        nombre = this.nombre,
        rol = this.rol
    )
}