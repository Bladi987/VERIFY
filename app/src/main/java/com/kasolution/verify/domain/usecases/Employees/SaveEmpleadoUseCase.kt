package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.data.repository.EmpleadoRepository

class SaveEmpleadoUseCase(val repository: EmpleadoRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(empleado: Employee, pass: String, requestId: String) {
        repository.saveEmpleado(empleado, pass,requestId)
    }
}