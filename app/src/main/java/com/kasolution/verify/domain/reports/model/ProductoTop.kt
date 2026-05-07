package com.kasolution.verify.domain.reports.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductoTop(
    val nombre: String,
    val cantidad_vendida: Int,
    val total_recaudado: Double
) : Parcelable