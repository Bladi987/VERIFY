package com.kasolution.verify.UI.branch.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.domain.branch.model.Branch
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Branch.*
import java.util.UUID

class SucursalViewModel(
    private val getBranchesUseCase: GetBranchesUseCase,
    private val saveBranchUseCase: SaveBranchUseCase,
    private val updateBranchUseCase: UpdateBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "SucursalViewModel"
    private var currentRequestId: String? = null

    private val _branchesList = MutableLiveData<List<Branch>>()
    val branchesList: LiveData<List<Branch>> get() = _branchesList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        getBranchesUseCase.repository.registerObserver()
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Sucursal)")
            loadBranches()
        }

        if (socketManager.isConnected) {
            loadBranches()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getBranchesUseCase.repository

        repo.onBranchesListReceived = { lista ->
            _branchesList.postValue(lista)
            _isLoading.postValue(false)
        }

        val resultHandler: (String, Boolean, String?) -> Unit = { accion, exito, requestIdRecibido ->
            _isLoading.postValue(false)

            if (exito) {
                if (requestIdRecibido == currentRequestId) {
                    _operationSuccess.postValue(accion)
                    currentRequestId = null
                }
                loadBranches()
            } else {
                if (requestIdRecibido == currentRequestId) {
                    exception.postValue("Error en operación Sucursal: $accion")
                    currentRequestId = null
                }
            }
        }

        repo.onOperationResult = resultHandler
    }

    fun loadBranches() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getBranchesUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }

    fun saveBranch(nombre: String, ruc: String, direccion: String, telefono: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val branch = Branch(0, nombre, ruc, direccion, telefono, estado)
        saveBranchUseCase(branch, currentRequestId!!)
    }

    fun updateBranch(id: Int, nombre: String, ruc: String, direccion: String, telefono: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val branch = Branch(id, nombre, ruc, direccion, telefono, estado)
        updateBranchUseCase(branch, currentRequestId!!)
    }

    fun deleteBranch(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteBranchUseCase(id, currentRequestId!!)
    }

    override fun onCleared() {
        super.onCleared()
        getBranchesUseCase.repository.clear()
    }
}