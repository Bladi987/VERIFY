package com.kasolution.verify.domain.usecases.Clients

import com.kasolution.verify.data.repository.ClientsRepository

class GetClientsUseCase(val repository: ClientsRepository) {
    // Los casos de uso suelen tener una única función principal
    operator fun invoke() {
        repository.getClients()
    }
}