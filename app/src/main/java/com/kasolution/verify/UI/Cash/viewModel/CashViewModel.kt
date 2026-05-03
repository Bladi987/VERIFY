package com.kasolution.verify.UI.Cash.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Cash.*
import java.util.UUID

class CashViewModel(
    private val sessionManager: SessionManager,
    private val openCashUseCase: OpenCashUseCase,
    private val closeCashUseCase: CloseCashUseCase,
    private val getCashStatusUseCase: GetCashStatusUseCase,
    private val addCashMovementUseCase: AddCashMovementUseCase,
    private val getCashHistoryUseCase: GetCashHistoryUseCase,
    private val socketManager: SocketManager
) : ViewModel() {
    private val tag = "CashViewModel"

    // --- LIVE DATA DE DATOS ---
    private val _cashHistory = MutableLiveData<List<Map<String, Any>>>()
    val cashHistory: LiveData<List<Map<String, Any>>> get() = _cashHistory

    val saldoInicial = _cashHistory.map { lista ->
        lista.find { it["tipo"] == "APERTURA" }?.get("monto")?.toString()?.toDoubleOrNull() ?: 0.0
    }

    val totalVentas = _cashHistory.map { lista ->
        lista.filter {
            val tipo = it["tipo"].toString()
            tipo == "VENTA" || tipo == "EFECTIVO" || tipo == "TARJETA" ||
                    tipo == "TRANSFERENCIA" || tipo == "YAPE" || tipo == "PLIN"
        }.sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }
    val totalYape = _cashHistory.map { lista ->
        lista.filter { it["tipo"] == "YAPE" }
            .sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }

    val totalPlin = _cashHistory.map { lista ->
        lista.filter { it["tipo"] == "PLIN" }
            .sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }

    val totalTarjeta = _cashHistory.map { lista ->
        lista.filter { it["tipo"] == "TARJETA" }
            .sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }

    val totalTransferencia = _cashHistory.map { lista ->
        lista.filter { it["tipo"] == "TRANSFERENCIA" }
            .sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }
    val totalEgresos = _cashHistory.map { lista ->
        lista.filter {
            val tipo = it["tipo"].toString()
            tipo == "EGRESO" || tipo == "COMPRA"
        }.sumOf { it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0 }
    }

    val totalAjustes = _cashHistory.map { lista ->
        // "Otros Movimientos" son ingresos/egresos manuales que NO son ventas ni compras
        lista.filter {
            val tipo = it["tipo"].toString()
            val motivo = it["motivo"].toString().lowercase()
            (tipo == "INGRESO" && !motivo.contains("venta")) ||
                    (tipo == "EGRESO" && !motivo.contains("compra") && !motivo.contains("venta"))
        }.sumOf {
            val monto = it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
            if (it["tipo"] == "INGRESO") monto else -monto
        }
    }

    val saldoEsperado = _cashHistory.map { lista ->
        var total = 0.0
        lista.forEach {
            val monto = it["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
            val tipo = it["tipo"].toString()

            when (tipo) {
                "APERTURA", "INGRESO", "VENTA", "EFECTIVO" -> total += monto

                "EGRESO", "COMPRA" -> total -= monto
            }
        }
        total
    }

    // --- DATOS DE SESIÓN Y ESTADO ---
    private val _closeReport = MutableLiveData<Map<String, Any>?>()
    val closeReport: LiveData<Map<String, Any>?> get() = _closeReport
    val userId: Int = sessionManager.getUserId()
    val userRol: String = sessionManager.getUserRole()
    val userName: String = sessionManager.getUserName()
    private val _activeSessionId = MutableLiveData<Int>(sessionManager.getActiveCashSessionId())
    val activeSessionId: LiveData<Int> get() = _activeSessionId

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()
    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        openCashUseCase.repository.registerObserver()
        setupRepositoryObservers()
        checkCurrentStatus()

        socketManager.onConnected = {
            checkCurrentStatus()
            loadHistory()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = openCashUseCase.repository

        repo.onCashStatusReceived = { data ->
            val idSesion = (data?.get("id_sesion") as? Number)?.toInt() ?: 0
            Log.d("CashViewModel", "Sincronizando con Servidor: ID recibido = $idSesion")
            updateLocalSession(idSesion)
            if (idSesion > 0) loadHistory()
            _isLoading.postValue(false)
        }

        repo.onCashHistoryReceived = { lista ->
            _cashHistory.postValue(lista)
            _isLoading.postValue(false)
        }

        repo.onOperationResult = { accion, exito, message ->
            _isLoading.postValue(false)
            if (exito) {
                when (accion) {
                    "CASH_OPEN", "CASH_OPEN_AUTHORIZED" -> {
                        checkCurrentStatus()
                    }

                    "CASH_CLOSE" -> {
                        // Importante: No disparamos el éxito aquí todavía,
                        // esperamos a que llegue el reporte completo en onCashCloseReportReceived

                        Log.d(tag, "Cierre solicitado con éxito, esperando reporte...")
                        Log.d(tag, "mensaje de reporte: $message")
                    }

                    "CASH_MOVEMENT" -> {
                        loadHistory() // Recargar lista tras un ingreso/egreso
                    }
                }
                _operationSuccess.postValue(accion)
            } else {
                if (message?.contains("Ya tienes", true) == true) {
                    Log.w(tag, "Conflicto: La sesión ya existía en el servidor. Sincronizando...")
                    exception.postValue("Error")
                    checkCurrentStatus()
                } else {
                    // Es un error real (ej. "Monto inválido")
                    exception.postValue(message ?: "Error desconocido")
                }
            }
        }
        repo.onCashCloseReportReceived = { report ->
            Log.d(tag, "Reporte de cierre recibido: $report")
            _closeReport.postValue(report)
            // Al cerrar con éxito, limpiamos la sesión localmente
            updateLocalSession(0)
            // Opcional: Limpiar el historial para que la vista se vacíe al cerrar
            _cashHistory.postValue(emptyList())
            _operationSuccess.postValue("CASH_CLOSE_SUCCESS")
            _isLoading.postValue(false)
        }
    }

    // --- ACCIONES DIRECTAS ---

    fun checkCurrentStatus() {
        if (socketManager.isConnected) {
            getCashStatusUseCase(userId)
        }
    }

    fun loadHistory() {
        val idSesion = sessionManager.getActiveCashSessionId()
        if (idSesion > 0 && socketManager.isConnected) {
            _isLoading.value = true
            getCashHistoryUseCase(idSesion, UUID.randomUUID().toString())
        }
    }

    fun openCash(montoInicial: Double) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        openCashUseCase(userId, montoInicial, UUID.randomUUID().toString())
    }

    fun openCashAuthorized(montoInicial: Double, superUser: String, superPass: String) {
        if (_isLoading.value == true) return
        _isLoading.value = true

        openCashUseCase.repository.authorizeAndOpenCash(
            userId,
            montoInicial,
            superUser,
            superPass,
            UUID.randomUUID().toString()
        )
    }

    fun closeCash(montoRealEnCaja: Double) {
        val idSesion = sessionManager.getActiveCashSessionId()
        if (idSesion <= 0) {
            exception.postValue("No hay una sesión activa para cerrar")
            return
        }
        _isLoading.value = true
        closeCashUseCase(idSesion, montoRealEnCaja, UUID.randomUUID().toString())
    }

    fun addMovement(tipo: String, monto: Double, motivo: String) {
        val idSesion = sessionManager.getActiveCashSessionId()
        if (idSesion <= 0) {
            exception.postValue("Debe abrir caja para registrar movimientos")
            return
        }
        _isLoading.value = true
        addCashMovementUseCase(idSesion, tipo, monto, motivo, UUID.randomUUID().toString())
    }

    // --- GESTIÓN DE SESIÓN LOCAL ---

    private fun updateLocalSession(idSesion: Int) {
        if (idSesion > 0) {
            sessionManager.saveActiveCashSessionId(idSesion)
        } else {
            sessionManager.clearCashSession()
        }
        _activeSessionId.postValue(idSesion)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _closeReport.value = null
    }

    fun isAuditorMode(): Boolean = userRol == "ADMIN"

    fun needsSupervisorToOpen(): Boolean = userRol == "ALMACEN"

    override fun onCleared() {
        super.onCleared()
        openCashUseCase.repository.clear()
    }
}