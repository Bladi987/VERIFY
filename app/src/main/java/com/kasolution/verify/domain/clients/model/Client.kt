package com.kasolution.verify.domain.clients.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Client(
    val id: Int,
    val nombre: String,
    val dniRuc: String,
    val telefono: String,
    val email: String,
    val direccion: String
): Parcelable