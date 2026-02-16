package com.kasolution.verify.UI.Category.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Category(
    @SerializedName("id_categoria") val id: Int = 0,
    @SerializedName ("nombre") val nombre: String,
    @SerializedName ("descripcion") val descripcion: String?=null,
    @SerializedName ("estado") val estado: Boolean=true,
): Serializable
