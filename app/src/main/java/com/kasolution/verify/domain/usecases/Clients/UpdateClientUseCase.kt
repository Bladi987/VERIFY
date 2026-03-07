package com.kasolution.verify.domain.usecases.Clients

import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.data.repository.ClientsRepository

class UpdateClientUseCase(val repository: ClientsRepository) {
    operator fun invoke(cliente: Client, requestId: String) {
        repository.updateClient(cliente,requestId)
    }
}