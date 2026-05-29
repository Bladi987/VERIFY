package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("action") val action: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: AuthDataDto?
)

data class AuthDataDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("sucursal_id") val idSucursal: Int?,
    @SerializedName("sucursal_nombre") val sucursalNombre: String?,
    @SerializedName("rol") val rol: RolDto?,
    @SerializedName("modulos") val modulos: List<ModuloDto>?,
    @SerializedName("permisos") val permisos: List<String>?
)

data class RolDto(
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("nombre_rol") val nombreRol: String?,
    @SerializedName("slug_rol") val slugRol: String?
)

data class ModuloDto(
    @SerializedName("id_modulo") val idModulo: Int,
    @SerializedName("nombre_modulo") val nombreModulo: String?,
    @SerializedName("slug_modulo") val slugModulo: String?,
    @SerializedName("icono") val icono: String?,
    @SerializedName("ruta") val ruta: String?,
    @SerializedName("orden") val orden: Int
)