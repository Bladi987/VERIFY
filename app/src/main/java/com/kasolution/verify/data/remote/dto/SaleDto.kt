package com.kasolution.verify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SaleDto(
    @SerializedName("id_venta") val id: Any? = null,
    @SerializedName("id_cliente") val idCliente: Any? = null,
    @SerializedName("id_empleado") val idEmpleado: Any? = null,
    @SerializedName("total") val total: Any? = null,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("estado") val estado: String? = "PAGADO"
)