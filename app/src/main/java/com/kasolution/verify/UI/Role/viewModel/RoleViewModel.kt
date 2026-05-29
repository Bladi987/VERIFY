package com.kasolution.verify.UI.Role.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.role.model.Role
import com.kasolution.verify.domain.usecases.Roles.DeleteRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.GetRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.SaveRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.UpdateRoleUseCase
import java.util.UUID

class RoleViewModel(
    private val getRolesUseCase: GetRoleUseCase,
    private val saveRoleUseCase: SaveRoleUseCase,
    private val updateRoleUseCase: UpdateRoleUseCase,
    private val deleteRoleUseCase: DeleteRoleUseCase,
    private val socketManager: SocketManager
) : ViewModel() {
    private val TAG = "RolesViewModel"
    private var currentRequestId: String? = null

    private val _roleList = MutableLiveData<List<Role>>()
    val rolesList: LiveData<List<Role>> get() = _roleList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        getRolesUseCase.repository.registerObserver()
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (roles)")
            loadRoles()
        }

        if (socketManager.isConnected) {
            loadRoles()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getRolesUseCase.repository

        repo.onRolesListReceived = { lista ->
            _roleList.postValue(lista)
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
                    loadRoles()
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Sucursal: $accion")
                        currentRequestId = null
                    }
                }
            }

        repo.onOperationResult = resultHandler
    }

    fun loadRoles() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getRolesUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }

    fun saveRole(nombre: String, slug: String, descripcion: String?, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()

        val role = Role(
            id = 0,
            nombre = nombre,
            slug = slug.toUpperCase().trim(),
            descripcion = descripcion?.trim(),
            estado = estado
        )
        saveRoleUseCase(role, currentRequestId!!)
    }

    fun updateRole(id: Int, nombre: String, slug: String, descripcion: String?, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()

        val role = Role(
            id = id,
            nombre = nombre,
            slug = slug.toUpperCase().trim(),
            descripcion = descripcion?.trim(),
            estado = estado
        )
        updateRoleUseCase(role, currentRequestId!!)
    }

    fun deleteRole(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteRoleUseCase(id, currentRequestId!!)
    }

    override fun onCleared() {
        super.onCleared()
        getRolesUseCase.repository.clear()
    }
}