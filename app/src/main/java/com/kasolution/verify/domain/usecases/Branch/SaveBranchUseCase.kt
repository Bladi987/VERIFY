package com.kasolution.verify.domain.usecases.Branch

import com.kasolution.verify.data.repository.SucursalRepository
import com.kasolution.verify.domain.branch.model.Branch

class SaveBranchUseCase(val repository: SucursalRepository) {
    operator fun invoke(branch: Branch, requestId: String) = repository.saveBranch(branch, requestId)
}