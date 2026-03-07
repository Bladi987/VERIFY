package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SaleDetailDto(
    @SerializedName("id_venta") val idVenta: Any? = null,
    @SerializedName("id_producto") val idProducto: Any? = null,
    @SerializedName("cantidad") val cantidad: Any? = null,
    @SerializedName("precio") val precio: Any? = null
)