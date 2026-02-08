package com.kasolution.verify.UI.Clients.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.UI.Clientes.viewModel.ClientesViewModel
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Clients.DeleteClientUseCase
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Clients.SaveClientUseCase
import com.kasolution.verify.domain.usecases.Clients.UpdateClientUseCase


class ClientesViewModelFactory(
    private val getClientesUseCase: GetClientsUseCase,
    private val saveClienteUseCase: SaveClientUseCase,
    private val updateClienteUseCase: UpdateClientUseCase,
    private val deleteClienteUseCase: DeleteClientUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientesViewModel(getClientesUseCase, saveClienteUseCase, updateClienteUseCase, deleteClienteUseCase, socketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}