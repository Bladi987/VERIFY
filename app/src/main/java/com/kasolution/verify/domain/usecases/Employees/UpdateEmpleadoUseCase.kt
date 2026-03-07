package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.data.repository.EmpleadoRepository

class UpdateEmpleadoUseCase(val repository: EmpleadoRepository) {
    operator fun invoke(empleado: Employee, pass: String?, requestId: String) {
        repository.updateEmpleado(empleado, pass,requestId)
    }
}