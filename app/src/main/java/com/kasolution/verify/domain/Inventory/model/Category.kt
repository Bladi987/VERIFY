package com.kasolution.verify.domain.Inventory.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val estado: Boolean
): Parcelable