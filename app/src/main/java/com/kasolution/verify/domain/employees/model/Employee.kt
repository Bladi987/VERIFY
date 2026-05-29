package com.kasolution.verify.domain.employees.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Employee(
    val id: Int,
    val nombre: String,
    val usuario: String,
    val correo: String?,
    val telefono: String?,
    val idSucursalBase: Int,
    val sucursalNombre: String,
    val idRol: Int,
    val nombreRol: String,
    val rolSlug: String,
    val estado: Boolean,
    val ultimoLogin: String?,
    val createdAt: String?
) : Parcelable {
    val initials: String
        get() = nombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
}