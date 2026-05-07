package com.kasolution.verify.UI.Reports.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.reports.GetCajaMovimientosUseCase
import com.kasolution.verify.domain.usecases.reports.GetInventarioResumenUseCase
import com.kasolution.verify.domain.usecases.reports.GetMetodosPagoUseCase
import com.kasolution.verify.domain.usecases.reports.GetTopProductosUseCase
import com.kasolution.verify.domain.usecases.reports.GetVentasUtilidadUseCase

class ReportesViewModelFactory(
    private val sesionManager: SessionManager,
    private val getVentasUtilidadUseCase: GetVentasUtilidadUseCase,
    private val getTopProductosUseCase: GetTopProductosUseCase,
    private val getMetodosPagoUseCase: GetMetodosPagoUseCase,
    private val getInventarioResumenUseCase: GetInventarioResumenUseCase,
    private val getCajaMovimientosUseCase: GetCajaMovimientosUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportesViewModel(
                sesionManager,
                getVentasUtilidadUseCase,
                getTopProductosUseCase,
                getMetodosPagoUseCase,
                getInventarioResumenUseCase,
                getCajaMovimientosUseCase,
                socketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}