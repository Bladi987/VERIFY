package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SupplierDto(
    @SerializedName("id_proveedor") val id: Any? = null, // Puede venir como 1 o "1"
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("direccion") val direccion: String? = null
)