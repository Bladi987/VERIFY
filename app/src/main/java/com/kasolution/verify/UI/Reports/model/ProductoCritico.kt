package com.kasolution.verify.UI.Reports.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductoCritico(
    val nombre: String,
    val stock_actual: Int,
    val stock_minimo: Int,
    val estado:String
): Parcelable