package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.UI.Employees.model.Empleado
import com.kasolution.verify.data.repository.EmpleadoRepository

class UpdateEmpleadoUseCase(val repository: EmpleadoRepository) {
    operator fun invoke(empleado: Empleado, pass: String?, requestId: String) {
        repository.updateEmpleado(empleado, pass,requestId)
    }
}