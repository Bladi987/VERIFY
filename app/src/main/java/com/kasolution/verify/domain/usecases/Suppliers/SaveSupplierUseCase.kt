package com.kasolution.verify.domain.usecases.Suppliers

import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.data.repository.SuppliersRepository


class SaveSupplierUseCase(val repository: SuppliersRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(supplier: Supplier, requestId: String) {
        repository.saveSupplier(supplier,requestId)
    }
}