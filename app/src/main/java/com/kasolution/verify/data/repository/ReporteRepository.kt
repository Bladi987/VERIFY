package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.reports.model.*

class ReporteRepository(private val socketManager: SocketManager) {

    private val TAG = "ReporteRepository"
    private val gson = Gson()

    // --- Callbacks para el ViewModel ---
    var onVentasUtilidadReceived: ((List<ReporteVenta>) -> Unit)? = null
    var onTopProductosReceived: ((List<ProductoTop>) -> Unit)? = null
    var onMetodosPagoReceived: ((List<ReporteMetodoPago>) -> Unit)? = null
    var onInventarioResumenReceived: ((ReporteInventario) -> Unit)? = null
    var onCajaMovimientosReceived: ((List<ReporteCajaMovimiento>) -> Unit)? = null
    var onOperationError: ((String) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        Log.d(TAG, "Registrando observer de ReporteRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                val action = jsonObject.get("action")?.asString ?: ""
                val status = jsonObject.get("status")?.asString == "success"

                if (!status) {
                    val msg = jsonObject.get("message")?.asString ?: "Error desconocido"
                    Handler(Looper.getMainLooper()).post { onOperationError?.invoke(msg) }
                    return@addObserver
                }

                when (action) {
                    "REPORTE_VENTAS_UTILIDAD" -> {
                        val type = object : TypeToken<SocketResponse<List<ReporteVenta>>>() {}.type
                        val response: SocketResponse<List<ReporteVenta>> = gson.fromJson(json, type)
                        Handler(Looper.getMainLooper()).post {
                            onVentasUtilidadReceived?.invoke(
                                response.data ?: emptyList()
                            )
                        }
                    }

                    "REPORTE_TOP_PRODUCTOS" -> {
                        val type = object : TypeToken<SocketResponse<List<ProductoTop>>>() {}.type
                        val response: SocketResponse<List<ProductoTop>> = gson.fromJson(json, type)
                        Handler(Looper.getMainLooper()).post {
                            onTopProductosReceived?.invoke(
                                response.data ?: emptyList()
                            )
                        }
                    }

                    "REPORTE_METODOS_PAGO" -> {
                        val type =
                            object : TypeToken<SocketResponse<List<ReporteMetodoPago>>>() {}.type
                        val response: SocketResponse<List<ReporteMetodoPago>> =
                            gson.fromJson(json, type)
                        Handler(Looper.getMainLooper()).post {
                            onMetodosPagoReceived?.invoke(
                                response.data ?: emptyList()
                            )
                        }
                    }

                    "REPORTE_INVENTARIO_RESUMEN" -> {
                        val type = object : TypeToken<SocketResponse<ReporteInventario>>() {}.type
                        val response: SocketResponse<ReporteInventario> = gson.fromJson(json, type)
                        response.data?.let { data ->
                            Handler(Looper.getMainLooper()).post {
                                onInventarioResumenReceived?.invoke(
                                    data
                                )
                            }
                        }
                    }

                    "REPORTE_CAJA_MOVIMIENTOS" -> {
                        val type = object :
                            TypeToken<SocketResponse<List<ReporteCajaMovimiento>>>() {}.type
                        val response: SocketResponse<List<ReporteCajaMovimiento>> =
                            gson.fromJson(json, type)
                        Handler(Looper.getMainLooper()).post {
                            onCajaMovimientosReceived?.invoke(
                                response.data ?: emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando mensaje", e)
            }
        }
    }

    /* =========================
       ACCIONES (PETICIONES)
       ========================= */

    fun getVentasUtilidad(fechaInicio: String, fechaFin: String, requestId: String) {
        socketManager.sendAction(
            "REPORTE_VENTAS_UTILIDAD",
            mapOf("fecha_inicio" to fechaInicio, "fecha_fin" to fechaFin),
            requestId
        )
    }

    fun getTopProductos(fechaInicio: String, fechaFin: String, requestId: String) {
        socketManager.sendAction(
            "REPORTE_TOP_PRODUCTOS",
            mapOf("fecha_inicio" to fechaInicio, "fecha_fin" to fechaFin),
            requestId
        )
    }

    fun getMetodosPago(fechaInicio: String, fechaFin: String, requestId: String) {
        socketManager.sendAction(
            "REPORTE_METODOS_PAGO",
            mapOf("fecha_inicio" to fechaInicio, "fecha_fin" to fechaFin),
            requestId
        )
    }

    fun getInventarioResumen(requestId: String) {
        socketManager.sendAction("REPORTE_INVENTARIO_RESUMEN", emptyMap(), requestId)
    }

    fun getCajaMovimientos(fechaInicio: String, fechaFin: String, requestId: String) {
        socketManager.sendAction(
            "REPORTE_CAJA_MOVIMIENTOS",
            mapOf("fecha_inicio" to fechaInicio, "fecha_fin" to fechaFin),
            requestId
        )
    }

    fun clear() {
        socketManager.removeObserver(TAG)
        onVentasUtilidadReceived = null
        onTopProductosReceived = null
        onMetodosPagoReceived = null
        onInventarioResumenReceived = null
        onCajaMovimientosReceived = null
        onOperationError = null
    }
}