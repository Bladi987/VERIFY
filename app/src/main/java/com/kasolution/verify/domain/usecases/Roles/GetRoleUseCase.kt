package com.kasolution.verify.domain.usecases.Roles

import com.kasolution.verify.data.repository.RolRepository

class GetRoleUseCase(val repository: RolRepository) {
    operator fun invoke() {
        repository.getRoles()
    }
}