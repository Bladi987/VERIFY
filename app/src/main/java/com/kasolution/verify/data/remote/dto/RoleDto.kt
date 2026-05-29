package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RoleDto(
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("nombre_rol") val nombreRol: String,
    @SerializedName("slug_rol") val slugRol: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("estado") val estado: Boolean
)