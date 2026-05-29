package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.EmployeeDto
import com.kasolution.verify.data.remote.dto.EmployeePermissionDto
import com.kasolution.verify.data.remote.dto.RolEmpleadoDto
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.domain.employees.model.EmployeePermission

fun EmployeeDto.toDomain(): Employee {
    return Employee(
        id = this.idEmpleado, // Corregido de id_empleado a camelCase según el nuevo DTO
        nombre = this.nombre ?: "Empleado Sin Nombre",
        usuario = this.usuario ?: "sin_usuario",
        correo = this.correo,
        telefono = this.telefono,
        idSucursalBase = this.idSucursalBase ?: -1,
        sucursalNombre = this.sucursalNombre ?: "Global",
        idRol = this.rol?.idRol ?: -1,
        nombreRol = this.rol?.nombreRol ?: "Sin Rol",
        rolSlug = this.rol?.slugRol ?: "INVITADO",
        estado = this.estado,
        ultimoLogin = this.ultimoLogin,
        createdAt = this.createdAt
    )
}

fun Employee.toDto(): EmployeeDto {
    return EmployeeDto(
        idEmpleado = this.id,
        nombre = this.nombre,
        usuario = this.usuario,
        correo = this.correo,
        telefono = this.telefono,
        idSucursalBase = this.idSucursalBase,
        sucursalNombre = this.sucursalNombre,
        estado = this.estado,
        ultimoLogin = this.ultimoLogin,
        createdAt = this.createdAt,
        rol = RolEmpleadoDto(
            idRol = this.idRol,
            nombreRol = this.nombreRol,
            slugRol = this.rolSlug
        )
    )
}
fun EmployeePermissionDto.toPermissionDomain(): EmployeePermission {
    return EmployeePermission(
        idPermiso = this.idPermiso,
        modulo = this.modulo,
        nombre = this.nombre,
        slug = this.slug,
        heredadoDeRol = this.heredadoDeRol,
        excepcion = this.excepcion
    )
}
