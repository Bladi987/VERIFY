package com.kasolution.verify.domain.employees.model

data class EmployeePermission(
    val idPermiso: Int,
    val modulo: String,
    val nombre: String,
    val slug: String,
    val heredadoDeRol: Boolean,
    var excepcion: Boolean?
)