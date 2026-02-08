package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.data.repository.EmpleadoRepository

class GetEmpleadosUseCase( val repository: EmpleadoRepository) {
    // Los casos de uso suelen tener una única función principal
    operator fun invoke() {
        repository.getEmpleados()
    }
}