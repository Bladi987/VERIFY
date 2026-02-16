package com.kasolution.verify.UI.Sales.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase

class SalesViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val getClientsUseCase: GetClientsUseCase,
    private val socketManager: SocketManager
): ViewModel() {
    private val TAG ="SalesViewModel"
    private var currentRequestId: String? = null
    private val _productsList = MutableLiveData<List<Product>>()
    val productsList: LiveData<List<Product>> get() = _productsList
    private val _clientsList = MutableLiveData<List<Cliente>>()
    val clientsList: LiveData<List<Cliente>> get() = _clientsList
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()
    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Inventory)")
            getProductsUseCase.repository.onSocketReconnected()
        }

        if (socketManager.isConnected) {
            loadProducts()
        }
    }

    private fun setupRepositoryObservers() {
        val repoProducts = getProductsUseCase.repository
        val repoClients= getClientsUseCase.repository


        repoProducts.onInventoryListReceived = { lista ->
            Log.d(TAG, "Cantidad de Productos recibidos: ${lista.size}")
            _productsList.postValue(lista)
            _isLoading.postValue(false)
        }
        repoClients.onClientsListReceived={lista->
            Log.d(TAG, "Clientes recibidos: ${lista.size}")
            _clientsList.postValue(lista)
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
                    loadProducts()
                    //if (accion == "CATEGORY_SAVE") loadSuppliers()  //analizar bien si se utilizara en este contexto
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Inventory: $accion")
                        currentRequestId = null
                    }
                }
            }

        repoProducts.onOperationResult = resultHandler
    }

    fun loadProducts() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getProductsUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }
    fun loadClientes() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getClientsUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }


    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "InventoryViewModel destruido")
    }
}