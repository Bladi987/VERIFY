package com.kasolution.verify.UI.Employees.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.branch.model.Branch
import com.kasolution.verify.domain.employees.model.EmployeePermission
import com.kasolution.verify.domain.role.model.Role
import com.kasolution.verify.domain.usecases.Branch.GetBranchesUseCase
import com.kasolution.verify.domain.usecases.Employees.DeleteEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.UpdateEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.GetEmpleadosUseCase
import com.kasolution.verify.domain.usecases.Employees.SaveEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Roles.GetRoleUseCase
import java.util.UUID

class EmpleadosViewModel(
    private val getEmpleadosUseCase: GetEmpleadosUseCase,
    private val saveEmpleadoUseCase: SaveEmpleadoUseCase,
    private val updateEmpleadoUseCase: UpdateEmpleadoUseCase,
    private val deleteEmpleadoUseCase: DeleteEmpleadoUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getRolesUseCase: GetRoleUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "EmpleadosViewModel"
    private var currentRequestId: String? = null

    private val _empleadosList = MutableLiveData<List<Employee>>()
    val empleadosList: LiveData<List<Employee>> get() = _empleadosList
    private val _sucursalList = MutableLiveData<List<Branch>>()
    val sucuraslList: LiveData<List<Branch>> get() = _sucursalList
    private val _rolesList = MutableLiveData<List<Role>>()
    val roleslList: LiveData<List<Role>> get() = _rolesList
    private val _permisosEspecialesList = MutableLiveData<List<EmployeePermission>>()
    val permisosEspecialesList: LiveData<List<EmployeePermission>> get() = _permisosEspecialesList

    private val _savePermisosSuccess = MutableLiveData<Boolean>()
    val savePermisosSuccess: LiveData<Boolean> get() = _savePermisosSuccess
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        getEmpleadosUseCase.repository.registerObserver()
        getBranchesUseCase.repository.registerObserver()
        getRolesUseCase.repository.registerObserver()
        setupRepositoryObservers()
        socketManager.onConnected = {
            loadEmpleados(force = true)
            loadBranches()
            loadRoles()
        }
        if (socketManager.isConnected) {
            loadEmpleados()
            loadBranches()
            loadRoles()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getEmpleadosUseCase.repository
        val repoSucursal = getBranchesUseCase.repository
        val repoRole = getRolesUseCase.repository

        // 1. Callbacks de Listas limpios y directos (Idéntico a Inventario)
        repo.onEmpleadosListReceived = { lista ->
            Log.d(TAG, "onEmpleadosListReceived: Llegaron ${lista.size} empleados")
            _empleadosList.postValue(lista)
            _isLoading.postValue(false)
        }

        repoSucursal.onBranchesListReceived = { lista ->
            Log.d(TAG, "onBranchesListReceived: Llegaron ${lista.size} sucursales")
            val activas = lista.filter { it.estado }
            _sucursalList.postValue(activas)
        }
        repoRole.onRolesListReceived = { lista ->
            Log.d(TAG, "onRolesListReceived: Llegaron ${lista.size} roles")
            _rolesList.postValue(lista)
            _isLoading.postValue(false)
        }
        repo.onPermisosEspecialesReceived = { listaPermisos ->
            Log.d(
                TAG,
                "onPermisosEspecialesReceived: Llegaron ${listaPermisos.size} renglones de permisos"
            )
            _permisosEspecialesList.postValue(listaPermisos)
            _isLoading.postValue(false)
        }

        repo.onSavePermisosResult = { exito, mensaje ->
            _isLoading.postValue(false)
            if (exito) {
                _savePermisosSuccess.postValue(true)
            } else {
                exception.postValue(mensaje ?: "Error al actualizar los permisos especiales")
            }
        }

        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->
                _isLoading.postValue(false)

                if (exito) {
                    if (requestIdRecibido == currentRequestId) {
                        _operationSuccess.postValue(accion)
                        currentRequestId = null
                    }
                    when (accion) {
                        "EMPLEADO_SAVE", "EMPLEADO_UPDATE", "EMPLEADO_DELETE" -> loadEmpleados(force = true)
                        "BRANCH_GET_ALL" -> {
                            Log.d(TAG, "El servidor respondió exitosamente al GET de Sucursales")
                        }
                    }
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en servidor: $accion")
                        currentRequestId = null
                    }
                }
            }

        // 3. Asignación ordenada del handler a cada repositorio independiente
        repo.onOperationResult = resultHandler
        repoSucursal.onOperationResult = resultHandler
        repoRole.onOperationResult = resultHandler
        saveEmpleadoUseCase.repository.onOperationResult = resultHandler
        updateEmpleadoUseCase.repository.onOperationResult = resultHandler
        deleteEmpleadoUseCase.repository.onOperationResult = resultHandler
    }

    fun loadEmpleados(force: Boolean = false) {
        // Evitamos peticiones redundantes si ya está cargando, a menos que sea forzado
        if (_isLoading.value == true && !force) {
            return
        }

        _isLoading.postValue(true)

        if (socketManager.isConnected) {
            getEmpleadosUseCase()
        } else {
            Log.e(TAG, "Servidor desconectado")
            _isLoading.postValue(false)
            exception.postValue("Servidor desconectado")
        }
    }

    fun loadBranches() {
        if (socketManager.isConnected) {
            getBranchesUseCase()
        }
    }

    fun loadRoles() {
        if (socketManager.isConnected) {
            getRolesUseCase()
        }
    }

    fun loadPermisosEspeciales(idEmpleado: Int) {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getEmpleadosUseCase.repository.getPermisosEspeciales(idEmpleado)
        } else {
            _isLoading.postValue(false)
            exception.postValue("Servidor desconectado")
        }
    }


    fun savePermisosEspeciales(idEmpleado: Int, excepciones: List<Map<String, Any?>>) {
        _isLoading.postValue(true)
        currentRequestId =
            UUID.randomUUID().toString() // Reutiliza el generador para control de tráfico

        if (socketManager.isConnected) {
            getEmpleadosUseCase.repository.savePermisosEspeciales(
                idEmpleado,
                excepciones,
                currentRequestId!!
            )
        } else {
            _isLoading.postValue(false)
            exception.postValue("Servidor desconectado")
        }
    }

    fun resetSavePermisosStatus() {
        _savePermisosSuccess.value = false
    }

    fun saveEmpleado(
        nombre: String,
        usuario: String,
        correo: String?,
        telefono: String?,
        pass: String,
        pin: String?,
        idSucursal: Int,
        sucursalNombre: String,
        idRol: Int,
        nombreRol: String,
        rolSlug: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()

        val nuevoEmpleado = Employee(
            id = 0,
            nombre = nombre,
            usuario = usuario,
            correo = correo,
            telefono = telefono,
            idSucursalBase = idSucursal,
            sucursalNombre = sucursalNombre,
            idRol = idRol,
            nombreRol = nombreRol,
            rolSlug = rolSlug,
            estado = estado,
            ultimoLogin = null,
            createdAt = null
        )
        saveEmpleadoUseCase(nuevoEmpleado, pass, pin, currentRequestId!!)
    }

    fun updateEmpleado(
        id: Int,
        nombre: String,
        usuario: String,
        correo: String?,
        telefono: String?,
        pass: String?,
        pin: String?,
        idSucursal: Int,
        sucursalNombre: String,
        idRol: Int,
        nombreRol: String,
        rolSlug: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()

        val empleadoEditado = Employee(
            id = id,
            nombre = nombre,
            usuario = usuario,
            correo = correo,
            telefono = telefono,
            idSucursalBase = idSucursal,
            sucursalNombre = sucursalNombre,
            idRol = idRol,
            nombreRol = nombreRol,
            rolSlug = rolSlug,
            estado = estado,
            ultimoLogin = null, // Se preserva intacto en backend
            createdAt = null    // Se preserva intacto en backend
        )
        updateEmpleadoUseCase(empleadoEditado, pass, pin, currentRequestId!!)
    }

    fun deleteEmpleado(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteEmpleadoUseCase(id, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        getEmpleadosUseCase.repository.clear()
        getBranchesUseCase.repository.clear()
        getRolesUseCase.repository.clear()
    }
}