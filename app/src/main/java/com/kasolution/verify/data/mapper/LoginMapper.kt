package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.LoginResponseDto
import com.kasolution.verify.domain.access.model.LoginResult
import com.kasolution.verify.domain.access.model.User

fun LoginResponseDto.toDomain(): LoginResult {
    return if (this.status == "success" && this.data != null) {
        // Usamos el mapper de User que creamos antes
        LoginResult.Success(this.data.toDomain())
    } else {
        LoginResult.Error(this.message ?: "Error desconocido")
    }
}