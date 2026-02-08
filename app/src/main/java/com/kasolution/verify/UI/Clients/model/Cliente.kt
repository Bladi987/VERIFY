package com.kasolution.verify.UI.Clients.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Cliente(
    @SerializedName("id_cliente") val id: Int = 0,
    @SerializedName ("nombre") val nombre: String,
    @SerializedName ("dni_ruc") val dniRuc: String,
    @SerializedName ("telefono") val telefono: String,
    @SerializedName ("email") val email: String,
    @SerializedName ("direccion") val direccion: String
): Serializable