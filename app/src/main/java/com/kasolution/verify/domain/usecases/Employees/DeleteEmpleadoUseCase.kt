package com.kasolution.verify.domain.usecases.Employees

import com.kasolution.verify.data.repository.EmpleadoRepository

class DeleteEmpleadoUseCase(val repository: EmpleadoRepository) {
    operator fun invoke(id: Int, requestId: String) {
        repository.deleteEmpleado(id,requestId)
    }
}