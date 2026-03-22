package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.network.SocketManager

class PurchaseRepository(private val socketManager: SocketManager) {

    private val TAG = "PurchaseRepository"
    private val gson = Gson()

    // Callbacks para la UI
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null
    var onPurchaseHistoryReceived: ((List<Map<String, Any>>) -> Unit)? = null
    var onPurchaseDetailReceived: ((Map<String, Any>) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        Log.d(TAG, "Registrando observer de PurchaseRepository")
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

                // Capturamos el mensaje (útil para errores de validación en anulación)
                val message = if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull) {
                    jsonObject.get("message").asString
                } else null

                val requestId = if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull) {
                    jsonObject.get("request_id").asString
                } else null

                when (action) {
                    "PURCHASE_SAVE", "PURCHASE_DELETE" -> {
                        Log.d(TAG, "Respuesta de $action: exito=$status")
                        Handler(Looper.getMainLooper()).post {
                            // Priorizamos el mensaje del servidor para el feedback en UI
                            onOperationResult?.invoke(action, status, message ?: requestId)
                        }
                    }

                    "PURCHASE_GET_ALL" -> {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val response: Map<String, Any> = gson.fromJson(json, type)
                        val data = response["data"] as? List<Map<String, Any>> ?: emptyList()

                        Log.d(TAG, "Historial recibido. Items: ${data.size}")

                        Handler(Looper.getMainLooper()).post {
                            // El campo 'estado' ya viene dentro de cada Map en la lista
                            onPurchaseHistoryReceived?.invoke(data)
                        }
                    }

                    "PURCHASE_GET_DETAIL" -> {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val response: Map<String, Any> = gson.fromJson(json, type)
                        val data = response["data"] as? Map<String, Any> ?: emptyMap()

                        Handler(Looper.getMainLooper()).post {
                            onPurchaseDetailReceived?.invoke(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico en PurchaseRepository: ${e.message}")
            }
        }
    }

    fun savePurchase(
        idProveedor: Int,
        idEmpleado: Int,
        total: Double,
        detalles: List<Map<String, Any>>,
        requestId: String
    ) {
        val params = mapOf(
            "id_proveedor" to idProveedor,
            "id_empleado" to idEmpleado,
            "total" to total,
            "detalles" to detalles
        )
        socketManager.sendAction("PURCHASE_SAVE", params, requestId)
    }

    fun deletePurchase(idCompra: Int, requestId: String) {
        socketManager.sendAction(
            "PURCHASE_DELETE",
            mapOf("id_compra" to idCompra),
            requestId
        )
    }

    fun getPurchaseHistory() {
        socketManager.sendAction("PURCHASE_GET_ALL")
    }

    fun getPurchaseDetail(idCompra: Int) {
        socketManager.sendAction("PURCHASE_GET_DETAIL", mapOf("id_compra" to idCompra))
    }

    fun onSocketReconnected() {
        registerObserver()
    }

    fun clear() {
        Log.d(TAG, "Limpiando PurchaseRepository")
        socketManager.removeObserver(TAG)
        onOperationResult = null
        onPurchaseHistoryReceived = null
        onPurchaseDetailReceived = null
    }
}