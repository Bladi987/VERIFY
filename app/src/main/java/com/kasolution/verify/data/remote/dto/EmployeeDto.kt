package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmployeeDto(
    @SerializedName("id_empleado") val id: Int = 0,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("usuario") val usuario: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("estado") val estado: Boolean = true
)