package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.network.SocketManager

class SalesRepository(private val socketManager: SocketManager) {

    private val TAG = "SalesRepository"
    private val gson = Gson()

    // Callbacks para la UI
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null
    var onSalesHistoryReceived: ((List<Map<String, Any>>) -> Unit)? = null
    var onSaleDetailReceived: ((List<Map<String, Any>>) -> Unit)? = null
    var onInvoiceDataReceived: ((String) -> Unit)? = null // Callback para el paquete completo
    var lastGeneratedId: Int? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        Log.d(TAG, "Registrando observer de SalesRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                val status = if (jsonObject.has("status")) {
                    jsonObject.get("status").asString == "success"
                } else false

                val requestId = if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull) {
                    jsonObject.get("request_id").asString
                } else null

                when (action) {
                    // Implementación unificada para comprobantes dinámicos
                    "SALE_SAVE", "SALE_GET_DETAIL" -> {
                        Log.d(TAG, "Procesando comprobante: $action -> exito: $status")

                        if (status && jsonObject.has("data")) {
                            // Extraemos el objeto "data" (contiene business, header e items)
                            val dataJson = jsonObject.get("data").toString()

                            Handler(Looper.getMainLooper()).post {
                                // 1. Notificamos al ViewModel el objeto completo del comprobante
                                onInvoiceDataReceived?.invoke(dataJson)

                                // 2. Notificamos el resultado de la operación para lógica de UI (limpiar carrito, etc)
                                onOperationResult?.invoke(action, status, requestId)
                            }
                        } else {
                            // Si hay error, notificamos solo el resultado
                            Handler(Looper.getMainLooper()).post {
                                onOperationResult?.invoke(action, status, requestId)
                            }
                        }
                    }

                    "SALE_DELETE" -> {
                        Log.d(TAG, "Anulación procesada: $action -> exito: $status")
                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }

                    "SALE_GET_ALL" -> {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val response: Map<String, Any> = gson.fromJson(json, type)
                        val data = response["data"] as? List<Map<String, Any>> ?: emptyList()

                        Handler(Looper.getMainLooper()).post {
                            onSalesHistoryReceived?.invoke(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico en SalesRepository: ${e.message}")
            }
        }
    }

    /**
     * Procesa el registro de una venta completa.
     */
    fun saveSale(
        idCliente: Int?,
        idEmpleado: Int,
        total: Double,
        metodoPago: String,
        idTipoComprobante: Int, // CAMBIO: De String a Int para coincidir con la DB
        detalles: List<Map<String, Any>>,
        requestId: String
    ) {
        val params = mutableMapOf<String, Any>(
            "id_cliente" to (idCliente ?: 0),
            "id_empleado" to idEmpleado,
            "total" to total,
            "metodo_pago" to metodoPago.uppercase(),
            "id_tipo_comprobante" to idTipoComprobante,
            "detalles" to detalles
        )

        Log.d(TAG, "Enviando Venta al Socket: $params")
        socketManager.sendAction("SALE_SAVE", params, requestId)
    }

    fun getSalesHistory() {
        socketManager.sendAction("SALE_GET_ALL")
    }

    /**
     * Solicita los datos completos del comprobante (incluye config empresa)
     */
    fun getSaleDetail(idVenta: Int) {
        socketManager.sendAction("SALE_GET_DETAIL", mapOf("id_venta" to idVenta))
    }

    fun deleteSale(idVenta: Int, requestId: String) {
        socketManager.sendAction(
            "SALE_DELETE",
            mapOf("id_venta" to idVenta),
            requestId
        )
    }

    fun onSocketReconnected() {
        registerObserver()
    }

    fun clear() {
        Log.d(TAG, "Limpiando SalesRepository")
        socketManager.removeObserver(TAG)
        onOperationResult = null
        onSalesHistoryReceived = null
        onSaleDetailReceived = null
        onInvoiceDataReceived = null
    }
}