package com.kasolution.verify.domain.branch.model

data class Branch(
    val id: Int,
    val nombre: String,
    val ruc: String?,
    val direccion: String?,
    val telefono: String?,
    val estado: Boolean
) {
    override fun toString(): String = nombre
}