package com.kasolution.verify.domain.usecases.Suppliers

import android.util.Log
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.data.repository.SuppliersRepository

class UpdateSupplierUseCase(val repository: SuppliersRepository) {
    operator fun invoke(supplier: Supplier, requestId: String) {
        repository.updateSupplier(supplier,requestId)
    }
}