package com.kasolution.verify.domain.sales.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Sale(
    val id: Int = 0, // Añadimos ID por si luego quieres listar ventas
    val idCliente: Int,
    val idEmpleado: Int,
    val total: Double,
    val metodoPago: String,
    val estado: String = "PAGADO",
    val fecha: String? = null // Útil para reportes
): Parcelable