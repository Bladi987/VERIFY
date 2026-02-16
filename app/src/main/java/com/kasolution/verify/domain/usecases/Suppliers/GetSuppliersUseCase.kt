package com.kasolution.verify.domain.usecases.Suppliers

import com.kasolution.verify.data.repository.SuppliersRepository


class GetSuppliersUseCase(val repository: SuppliersRepository) {
    // Los casos de uso suelen tener una única función principal
    operator fun invoke() {
        repository.getSuppliers()
    }
}