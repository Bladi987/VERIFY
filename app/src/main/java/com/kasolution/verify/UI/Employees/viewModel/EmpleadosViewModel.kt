package com.kasolution.verify.UI.Employees.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.domain.employees.model.Employee
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

    private val _empleadosList = MutableLiveData<List<Employee>>()
    val empleadosList: LiveData<List<Employee>> get() = _empleadosList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        // 1. Reactivamos el observador del repositorio (Soluciona el problema de carga al reingresar)
        getEmpleadosUseCase.repository.registerObserver()

        // 2. Vinculamos los callbacks del repositorio con la UI
        setupRepositoryObservers()

        // 3. Manejo de reconexión
        socketManager.onConnected = {
            Log.d(TAG, "Socket reconectado (Empleados) → Actualizando...")
            loadEmpleados(force = true)
        }

        // 4. Carga inicial al entrar (Recuerda quitarlo del onCreate de la Activity)
        if (socketManager.isConnected) {
            loadEmpleados()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getEmpleadosUseCase.repository

        repo.onEmpleadosListReceived = { lista ->
            Log.d(TAG, "Llegaron ${lista.size} empleados al ViewModel")
            _empleadosList.postValue(lista)
            _isLoading.postValue(false)
        }

        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->
                _isLoading.postValue(false)

                if (requestIdRecibido == currentRequestId) {
                    currentRequestId = null

                    if (exito) {
                        _operationSuccess.postValue(accion)
                        // Recargamos la lista automáticamente tras un cambio exitoso
                        loadEmpleados(force = true)
                    } else {
                        exception.postValue("Error en la operación de Empleados: $accion")
                    }
                }
            }

        // Todos los casos de uso comparten el mismo repositorio, asignamos el handler a todos
        repo.onOperationResult = resultHandler
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

    fun saveEmpleado(nombre: String, usuario: String, pass: String, rol: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val nuevoEmpleado = Employee(0, nombre, usuario, rol, estado)
        saveEmpleadoUseCase(nuevoEmpleado, pass, currentRequestId!!)
    }

    fun updateEmpleado(id: Int, nombre: String, user: String, pass: String?, rol: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val empleadoEditado = Employee(id, nombre, user, rol, estado)
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
        Log.d(TAG, "Cerrando pantalla: Limpiando repositorio de Empleados")
        // 5. CRÍTICO: Matamos el observador para evitar Memory Leaks y bugs de duplicidad
        getEmpleadosUseCase.repository.clear()
    }
}