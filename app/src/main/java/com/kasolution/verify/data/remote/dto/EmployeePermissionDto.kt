package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmployeePermissionDto(
    @SerializedName("id_permiso") val idPermiso: Int,
    @SerializedName("modulo") val modulo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("heredado_de_rol") val heredadoDeRol: Boolean,
    @SerializedName("excepcion") val excepcion: Boolean?
)