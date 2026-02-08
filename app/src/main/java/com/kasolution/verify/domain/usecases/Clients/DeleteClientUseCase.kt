package com.kasolution.verify.domain.usecases.Clients

import com.kasolution.verify.data.repository.ClientsRepository


class DeleteClientUseCase(val repository: ClientsRepository) {
    operator fun invoke(id: Int, requestId: String) {
        repository.deleteClient(id,requestId)
    }
}