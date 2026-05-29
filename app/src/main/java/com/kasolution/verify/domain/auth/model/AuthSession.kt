package com.kasolution.verify.domain.auth.model
import java.io.Serializable

data class UserSession(
    val id: Int,
    val nombre: String,
    val idSucursal: Int,
    val sucursalNombre: String,
    val rolNombre: String,
    val rolSlug: String,
    val permisos: List<String>,
    val modulos: List<AppModule>
) : Serializable
data class AppModule(
    val id: Int,
    val nombre: String,
    val slug: String,
    val icono: String,
    val ruta: String,
    val orden: Int
) : Serializable