package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.BranchDto
import com.kasolution.verify.domain.branch.model.Branch

fun BranchDto.toDomain(): Branch {
    return Branch(
        id = this.id,
        nombre = this.nombre,
        ruc = this.ruc,
        direccion = this.direccion,
        telefono = this.telefono,
        estado = when (this.estado) {
            is Boolean -> this.estado
            is Number -> this.estado.toInt() == 1
            else -> false
        }
    )
}