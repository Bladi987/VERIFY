package com.kasolution.verify.UI.Suppliers.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Supplier(
    @SerializedName("id_proveedor") val id: Int = 0,
    @SerializedName ("nombre") val nombre: String,
    @SerializedName ("telefono") val telefono: String,
    @SerializedName ("email") val email: String,
    @SerializedName ("direccion") val direccion: String
): Serializable