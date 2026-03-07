package com.kasolution.verify.domain.employees.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Employee(
    val id: Int,
    val nombre: String,
    val usuario: String,
    val rol: String,
    val estado: Boolean
) : Parcelable {
    val initials: String
        get() = nombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
}