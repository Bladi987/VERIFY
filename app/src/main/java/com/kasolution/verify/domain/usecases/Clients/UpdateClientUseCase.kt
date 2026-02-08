package com.kasolution.verify.domain.usecases.Clients

import android.util.Log
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.data.repository.ClientsRepository

class UpdateClientUseCase(val repository: ClientsRepository) {
    operator fun invoke(cliente: Cliente, requestId: String) {
        repository.updateClient(cliente,requestId)
    }
}