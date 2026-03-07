package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.ClientDto
import com.kasolution.verify.domain.clients.model.Client

fun ClientDto.toDomain(): Client {
    return Client(
        id = this.id,
        nombre = this.nombre,
        dniRuc = this.dniRuc,
        telefono = this.telefono,
        email = this.email,
        direccion = this.direccion
    )
}

fun Client.toDto(): ClientDto {
    return ClientDto(
        id = this.id,
        nombre = this.nombre,
        dniRuc = this.dniRuc,
        telefono = this.telefono,
        email = this.email,
        direccion = this.direccion
    )
}