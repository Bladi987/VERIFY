package com.kasolution.verify.domain.reports.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReporteVenta(
    val fecha: String,
    val total_operaciones: Int,
    val ingresos_totales: Double,
    val utilidad_neta: Double
) : Parcelable