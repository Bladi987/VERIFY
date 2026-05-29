package com.kasolution.verify.domain.usecases.Roles

import com.kasolution.verify.data.repository.RolRepository
import com.kasolution.verify.domain.role.model.Role

class SaveRoleUseCase(private val repository: RolRepository) {
    operator fun invoke(role: Role, requestId: String) {
        repository.saveRole(role, requestId)
    }
}