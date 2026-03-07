package com.kasolution.verify.data.mapper

import com.kasolution.verify.data.remote.dto.EmployeeDto
import com.kasolution.verify.domain.employees.model.Employee

fun EmployeeDto.toDomain(): Employee {
    return Employee(
        id = this.id,
        nombre = this.nombre,
        usuario = this.usuario,
        rol = this.rol,
        estado = this.estado
    )
}

fun Employee.toDto(): EmployeeDto {
    return EmployeeDto(
        id = this.id,
        nombre = this.nombre,
        usuario = this.usuario,
        rol = this.rol,
        estado = this.estado
    )
}