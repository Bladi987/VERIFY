package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmployeeDto(
    @SerializedName("id_empleado") val idEmpleado: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("usuario") val usuario: String?,
    @SerializedName("correo") val correo: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("id_sucursal_base") val idSucursalBase: Int?,
    @SerializedName("sucursal_nombre") val sucursalNombre: String?,
    @SerializedName("estado") val estado: Boolean,
    @SerializedName("ultimo_login") val ultimoLogin: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("rol") val rol: RolEmpleadoDto?
)
data class RolEmpleadoDto(
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("nombre_rol") val nombreRol: String?,
    @SerializedName("slug_rol") val slugRol: String?
)