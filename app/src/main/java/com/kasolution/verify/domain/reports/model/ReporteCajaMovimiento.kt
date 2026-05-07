package com.kasolution.verify.domain.reports.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReporteCajaMovimiento(
    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("concepto")
    val concepto: String?,

    @SerializedName("fecha")
    val fecha: String?,

    @SerializedName("total")
    val monto: Double
) : Parcelable