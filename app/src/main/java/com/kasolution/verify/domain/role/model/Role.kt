package com.kasolution.verify.domain.role.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Role(
    val id: Int,
    val nombre: String,
    val slug: String,
    val descripcion: String?,
    val estado: Boolean
) : Parcelable {
    override fun toString(): String {
        return nombre
    }
}