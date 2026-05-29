package com.kasolution.verify.domain.usecases.Branch

import com.kasolution.verify.data.repository.SucursalRepository

class GetBranchesUseCase(val repository: SucursalRepository) {
    operator fun invoke() = repository.getBranches()
}