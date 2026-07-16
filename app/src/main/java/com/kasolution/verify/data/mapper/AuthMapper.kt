package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.AuthResponseDto
import com.kasolution.verify.domain.auth.model.AppModule
import com.kasolution.verify.domain.auth.model.AuthResult
import com.kasolution.verify.domain.auth.model.AuthSession

fun AuthResponseDto.toDomain(): AuthResult {
    if (this.status != "success" || this.data == null) {
        return AuthResult.Error(this.message ?: "Error de autenticación desconocido")
    }

    val dto = this.data
    val authSession = AuthSession(
        id = dto.id,
        nombre = dto.nombre ?: "Usuario",
        idSucursal = dto.idSucursal ?: 0,
        sucursalNombre = dto.sucursalNombre ?: "Sede no asignada",
        rolNombre = dto.rol?.nombreRol ?: "Sin Rol",
        rolSlug = dto.rol?.slugRol ?: "NONE",
        permisos = dto.permisos ?: emptyList(),
        modulos = dto.modulos?.map { moduloDto ->
            AppModule(
                id = moduloDto.idModulo,
                nombre = moduloDto.nombreModulo ?: "Módulo sin nombre",
                slug = moduloDto.slugModulo ?: "",
                icono = moduloDto.icono ?: "ic_default",
                ruta = moduloDto.ruta ?: "",
                orden = moduloDto.orden
            )
        }?.sortedBy { it.orden } ?: emptyList()
    )
    return AuthResult.Success(authSession)
}