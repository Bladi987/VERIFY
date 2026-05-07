package com.kasolution.verify.UI.Reports.viewModel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.reports.model.*
import com.kasolution.verify.domain.usecases.reports.*
import java.util.UUID

class ReportesViewModel(
    private val sesionManager: SessionManager,
    private val getVentasUtilidadUseCase: GetVentasUtilidadUseCase,
    private val getTopProductosUseCase: GetTopProductosUseCase,
    private val getMetodosPagoUseCase: GetMetodosPagoUseCase,
    private val getInventarioResumenUseCase: GetInventarioResumenUseCase,
    private val getCajaMovimientosUseCase: GetCajaMovimientosUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "ReportesViewModel"

    // --- LiveData de Datos ---
    private val _ventasUtilidad = MutableLiveData<List<ReporteVenta>>()
    val ventasUtilidad: LiveData<List<ReporteVenta>> get() = _ventasUtilidad

    private val _topProductos = MutableLiveData<List<ProductoTop>>()
    val topProductos: LiveData<List<ProductoTop>> get() = _topProductos

    private val _metodosPago = MutableLiveData<List<ReporteMetodoPago>>()
    val metodosPago: LiveData<List<ReporteMetodoPago>> get() = _metodosPago

    private val _inventarioResumen = MutableLiveData<ReporteInventario>()
    val inventarioResumen: LiveData<ReporteInventario> get() = _inventarioResumen

    private val _cajaMovimientos = MutableLiveData<List<ReporteCajaMovimiento>>()
    val cajaMovimientos: LiveData<List<ReporteCajaMovimiento>> get() = _cajaMovimientos

    // --- Estados de UI ---
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    val exception = MutableLiveData<String>()

    private var fechaInicioActual: String = ""
    private var fechaFinActual: String = ""

    init {
        getVentasUtilidadUseCase.repository.registerObserver()
        setupRepositoryObservers()

        socketManager.onConnected = {
            if (fechaInicioActual.isNotEmpty()) loadAllReportes(fechaInicioActual, fechaFinActual)
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getVentasUtilidadUseCase.repository

        repo.onVentasUtilidadReceived = { _ventasUtilidad.postValue(it); checkLoading() }
        repo.onTopProductosReceived = { _topProductos.postValue(it); checkLoading() }
        repo.onMetodosPagoReceived = { _metodosPago.postValue(it); checkLoading() }
        repo.onInventarioResumenReceived = { _inventarioResumen.postValue(it); checkLoading() }
        repo.onCajaMovimientosReceived = { _cajaMovimientos.postValue(it); checkLoading() }

        repo.onOperationError = {
            _isLoading.postValue(false)
            exception.postValue(it)
        }
    }

    fun loadAllReportes(fInicio: String, fFin: String) {
        if (_isLoading.value == true) return

        this.fechaInicioActual = fInicio
        this.fechaFinActual = fFin
        _isLoading.postValue(true)

        if (socketManager.isConnected) {
            // Disparamos todas las peticiones en paralelo
            getVentasUtilidadUseCase(fInicio, fFin, UUID.randomUUID().toString())
            getTopProductosUseCase(fInicio, fFin, UUID.randomUUID().toString())
            getMetodosPagoUseCase(fInicio, fFin, UUID.randomUUID().toString())
            getInventarioResumenUseCase(UUID.randomUUID().toString())
            getCajaMovimientosUseCase(fInicio, fFin, UUID.randomUUID().toString())
        } else {
            exception.postValue("Sin conexión al servidor")
            _isLoading.postValue(false)
        }
    }

    private fun checkLoading() {
        // En reportes, como son varias cargas, el loading se quita al recibir datos.
        // Podrías usar un contador si quieres que desaparezca solo cuando lleguen los 5.
        _isLoading.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        getVentasUtilidadUseCase.repository.clear()
    }
}