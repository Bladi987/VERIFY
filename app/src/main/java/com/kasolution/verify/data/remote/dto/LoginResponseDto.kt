package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.kasolution.verify.domain.access.model.User

data class LoginResponseDto(
    @SerializedName("action") val action: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserDto? // Usamos el DTO, no la Entity del dominio
)