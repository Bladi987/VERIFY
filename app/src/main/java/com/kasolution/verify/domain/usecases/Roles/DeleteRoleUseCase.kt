package com.kasolution.verify.domain.usecases.Roles

import com.kasolution.verify.data.repository.RolRepository

class DeleteRoleUseCase(private val repository: RolRepository) {
    operator fun invoke(idRol: Int, requestId: String) {
        repository.deleteRole(idRol, requestId)
    }
}