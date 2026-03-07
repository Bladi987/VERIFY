package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClientDto(
    @SerializedName("id_cliente") val id: Int = 0,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("dni_ruc") val dniRuc: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("email") val email: String,
    @SerializedName("direccion") val direccion: String
)