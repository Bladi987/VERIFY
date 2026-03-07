package com.kasolution.verify.domain.supplier.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
@Parcelize
data class Supplier(
    val id: Int,
    val nombre: String,
    val telefono: String,
    val email: String,
    val direccion: String
): Parcelable