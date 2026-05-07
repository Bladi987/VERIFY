package com.kasolution.verify.domain.reports.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReporteMetodoPago(val metodo: String, val cantidad: Int, val total: Double) : Parcelable