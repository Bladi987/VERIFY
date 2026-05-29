package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BranchDto(
    @SerializedName("id_sucursal") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("ruc") val ruc: String?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("estado") val estado: Any
)