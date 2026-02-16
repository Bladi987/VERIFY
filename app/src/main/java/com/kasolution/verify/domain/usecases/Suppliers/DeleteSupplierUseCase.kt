package com.kasolution.verify.domain.usecases.Suppliers

import com.kasolution.verify.data.repository.SuppliersRepository


class DeleteSupplierUseCase(val repository: SuppliersRepository) {
    operator fun invoke(id: Int, requestId: String) {
        repository.deleteSupplier(id,requestId)
    }
}