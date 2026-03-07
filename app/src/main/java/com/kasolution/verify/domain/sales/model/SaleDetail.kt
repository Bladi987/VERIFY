package com.kasolution.verify.domain.sales.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SaleDetail(
    val idVenta: Long,
    val idProducto: Int,
    val cantidad: Int,
    val precio: Double,
    val subtotal: Double = cantidad * precio
): Parcelable