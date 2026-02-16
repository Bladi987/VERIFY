package com.kasolution.verify.UI.Suppliers.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Suppliers.model.Supplier
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Suppliers.DeleteSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase
import com.kasolution.verify.domain.usecases.Suppliers.SaveSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.UpdateSupplierUseCase
import java.util.UUID

class SuppliersViewModel(
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val saveSupplierUseCase: SaveSupplierUseCase,
    private val updateSupplierUseCase: UpdateSupplierUseCase,
    private val deleteSupplierUseCase: DeleteSupplierUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "SuppliersViewModel"
    private var currentRequestId: String? = null

    private val _suppliersList = MutableLiveData<List<Supplier>>()
    val suppliersList: LiveData<List<Supplier>> get() = _suppliersList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Suppliers)")
            getSuppliersUseCase.repository.onSocketReconnected()
        }

        if (socketManager.isConnected) {
            loadSuppliers()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getSuppliersUseCase.repository

        repo.onSuppliersListReceived = { lista ->
            Log.d(TAG, "Suppliers recibidos: ${lista.size}")
            _suppliersList.postValue(lista)
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
                    loadSuppliers()
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Suppliers: $accion")
                        currentRequestId = null
                    }
                }
            }

        repo.onOperationResult = resultHandler
        saveSupplierUseCase.repository.onOperationResult = resultHandler
        updateSupplierUseCase.repository.onOperationResult = resultHandler
        deleteSupplierUseCase.repository.onOperationResult = resultHandler
    }

    fun loadSuppliers() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getSuppliersUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }

    fun saveSupplier(
        nombre: String,
        telefono: String,
        email: String,
        direccion: String
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val supplier = Supplier(0, nombre, telefono, email, direccion)
        saveSupplierUseCase(supplier, currentRequestId!!)
    }

    fun updateSupplier(
        id: Int,
        nombre: String,
        telefono: String,
        email: String,
        direccion: String
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val supplier = Supplier(id, nombre, telefono, email, direccion)
        updateSupplierUseCase(supplier, currentRequestId!!)
    }

    fun deleteSupplier(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteSupplierUseCase(id, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SuppliersViewModel destruido")
    }
}
