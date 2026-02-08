package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.UI.Employees.model.Empleado
import com.kasolution.verify.data.repository.EmpleadoRepository

class SaveEmpleadoUseCase(val repository: EmpleadoRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(empleado: Empleado, pass: String, requestId: String) {
        repository.saveEmpleado(empleado, pass,requestId)
    }
}