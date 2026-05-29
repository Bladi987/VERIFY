package com.kasolution.verify.domain.usecases.Branch

import com.kasolution.verify.data.repository.SucursalRepository

class DeleteBranchUseCase(val repository: SucursalRepository) {
    operator fun invoke(id: Int, requestId: String) = repository.deleteBranch(id, requestId)
}