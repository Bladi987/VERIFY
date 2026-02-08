package com.kasolution.verify.UI.Employees.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Employees.model.Empleado
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Employees.DeleteEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.UpdateEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.GetEmpleadosUseCase
import com.kasolution.verify.domain.usecases.Employees.SaveEmpleadoUseCase
import java.util.UUID

class EmpleadosViewModel(
    private val getEmpleadosUseCase: GetEmpleadosUseCase,
    private val saveEmpleadoUseCase: SaveEmpleadoUseCase,
    private val updateEmpleadoUseCase: UpdateEmpleadoUseCase,
    private val deleteEmpleadoUseCase: DeleteEmpleadoUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "EmpleadosViewModel"

    private var currentRequestId: String? = null

    private val _empleadosList = MutableLiveData<List<Empleado>>()
    val empleadosList: LiveData<List<Empleado>> get() = _empleadosList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        setupRepositoryObservers()

        // 🔧 CAMBIO CLAVE: SIEMPRE recargar al reconectar
        socketManager.onConnected = {
            Log.d(TAG, "Socket reconectado → recargando empleados")
            getEmpleadosUseCase.repository.onSocketReconnected()
            loadEmpleados(force = true)
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getEmpleadosUseCase.repository

        repo.onEmpleadosListReceived = { lista ->
            Log.d(TAG, "Llegaron ${lista.size} empleados al ViewModel")
            _empleadosList.postValue(lista)
            _isLoading.postValue(false)
        }

        // 🔧 IMPORTANTE: apagar loading ante errores
//        repo.onError = { error ->
//            Log.e(TAG, "Error obteniendo empleados: $error")
//            _isLoading.postValue(false)
//            exception.postValue(error ?: "Error al obtener empleados")
//        }

        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->

                _isLoading.postValue(false)

                if (requestIdRecibido == currentRequestId) {
                    currentRequestId = null

                    if (exito) {
                        _operationSuccess.postValue(accion)

                        // 🔧 Forzar recarga real
                        _empleadosList.postValue(emptyList())
                        loadEmpleados(force = true)

                    } else {
                        exception.postValue("Error en la operación de Empleados: $accion")
                    }
                }
            }

        repo.onOperationResult = resultHandler
        saveEmpleadoUseCase.repository.onOperationResult = resultHandler
        updateEmpleadoUseCase.repository.onOperationResult = resultHandler
        deleteEmpleadoUseCase.repository.onOperationResult = resultHandler
    }

    fun loadEmpleados(force: Boolean = false) {

        // 🔧 Evita bloqueos por loading eterno
        if (_isLoading.value == true && !force) {
            Log.w(TAG, "Carga ignorada: petición en curso")
            return
        }

        _isLoading.postValue(true)

        if (socketManager.isConnected) {
            Log.d(TAG, "Solicitando empleados al servidor")
            getEmpleadosUseCase()
        } else {
            Log.e(TAG, "Servidor desconectado")
            _isLoading.postValue(false)
            exception.postValue("Servidor desconectado")
        }
    }

    fun saveEmpleado(
        nombre: String,
        usuario: String,
        pass: String,
        rol: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val nuevoEmpleado = Empleado(0, nombre, usuario, rol, estado)
        saveEmpleadoUseCase(nuevoEmpleado, pass, currentRequestId!!)
    }

    fun updateEmpleado(
        id: Int,
        nombre: String,
        user: String,
        pass: String?,
        rol: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val empleadoEditado = Empleado(id, nombre, user, rol, estado)
        updateEmpleadoUseCase(empleadoEditado, pass, currentRequestId!!)
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
        Log.d(TAG, "Limpiando ViewModel y desuscribiendo...")
//        socketManager.unregisterConnectionListener()
//        getEmpleadosUseCase.repository.clear()
    }
}
