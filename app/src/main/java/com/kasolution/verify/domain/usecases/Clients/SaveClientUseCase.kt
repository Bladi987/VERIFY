package com.kasolution.verify.domain.usecases.Clients

import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.data.repository.ClientsRepository

class SaveClientUseCase(val repository: ClientsRepository) {
    // El UseCase ahora pide exactamente lo que el Repositorio necesita
    operator fun invoke(cliente: Cliente, requestId: String) {
        repository.saveClient(cliente,requestId)
    }
}