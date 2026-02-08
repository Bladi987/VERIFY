package com.kasolution.verify.UI.Clientes.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Clients.DeleteClientUseCase
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Clients.SaveClientUseCase
import com.kasolution.verify.domain.usecases.Clients.UpdateClientUseCase
import java.util.UUID

class ClientesViewModel(
    private val getClientesUseCase: GetClientsUseCase,
    private val saveClienteUseCase: SaveClientUseCase,
    private val updateClienteUseCase: UpdateClientUseCase,
    private val deleteClienteUseCase: DeleteClientUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "ClientesViewModel"
    private var currentRequestId: String? = null

    private val _clientesList = MutableLiveData<List<Cliente>>()
    val clientesList: LiveData<List<Cliente>> get() = _clientesList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Clientes)")
            getClientesUseCase.repository.onSocketReconnected()
        }

        if (socketManager.isConnected) {
            loadClientes()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getClientesUseCase.repository

        repo.onClientsListReceived = { lista ->
            Log.d(TAG, "Clientes recibidos: ${lista.size}")
            _clientesList.postValue(lista)
            _isLoading.postValue(false)
        }

        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->

                _isLoading.postValue(false)

                if (exito) {
                    if (requestIdRecibido == currentRequestId) {
                        _operationSuccess.postValue(accion)
                        currentRequestId = null
                    }
                    loadClientes()
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Clientes: $accion")
                        currentRequestId = null
                    }
                }
            }

        repo.onOperationResult = resultHandler
        saveClienteUseCase.repository.onOperationResult = resultHandler
        updateClienteUseCase.repository.onOperationResult = resultHandler
        deleteClienteUseCase.repository.onOperationResult = resultHandler
    }

    fun loadClientes() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getClientesUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }

    fun saveCliente(
        nombre: String,
        dniRuc: String,
        telefono: String,
        email: String,
        direccion: String
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val cliente = Cliente(0, nombre, dniRuc, telefono, email, direccion)
        saveClienteUseCase(cliente, currentRequestId!!)
    }

    fun updateCliente(
        id: Int,
        nombre: String,
        dniRuc: String,
        telefono: String,
        email: String,
        direccion: String
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val cliente = Cliente(id, nombre, dniRuc, telefono, email, direccion)
        updateClienteUseCase(cliente, currentRequestId!!)
    }

    fun deleteCliente(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteClienteUseCase(id, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ClientesViewModel destruido")
    }
}
