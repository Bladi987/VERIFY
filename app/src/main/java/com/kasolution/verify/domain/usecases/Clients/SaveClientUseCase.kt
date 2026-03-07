package com.kasolution.verify.domain.usecases.Clients

import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.data.repository.ClientsRepository

class SaveClientUseCase(val repository: ClientsRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(cliente: Client, requestId: String) {
        repository.saveClient(cliente,requestId)
    }
}