package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.network.SocketManager

class CashRepository(private val socketManager: SocketManager, private val sessionManager: SessionManager) {
    private val TAG = "CashRepository"
    private val gson = Gson()
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null
    var onCashStatusReceived: ((Map<String, Any>?) -> Unit)? = null
    var onCashHistoryReceived: ((List<Map<String, Any>>) -> Unit)? = null
    var onCashCloseReportReceived: ((Map<String, Any>) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        Log.d(TAG, "Registrando observer de CashRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                val action = jsonObject.get("action")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                val status = jsonObject.get("status")?.let { if (it.isJsonNull) false else it.asString == "success" } ?: false
                val message = jsonObject.get("message")?.let { if (it.isJsonNull) null else it.asString }
                val requestId = jsonObject.get("request_id")?.let { if (it.isJsonNull) null else it.asString }

                Log.d(TAG, "Procesando acción: $action con status: $status")

                when (action) {
                    "CASH_OPEN", "CASH_OPEN_AUTHORIZED", "CASH_ADD_MOVEMENT" -> {
                        val idSesion = jsonObject.get("id_sesion")?.asInt ?: 0
                        Handler(Looper.getMainLooper()).post {
                            if (status && idSesion > 0) {
                                val dummyData = mapOf("id_sesion" to idSesion.toDouble())
                                onCashStatusReceived?.invoke(dummyData)
                            }
                            onOperationResult?.invoke("CASH_OPEN", status, message ?: requestId)
                        }
                    }

                    "CASH_GET_STATUS" -> {
                        try {
                            val dataElement = jsonObject.get("data")
                            val data = if (dataElement != null && !dataElement.isJsonNull) {
                                val type = object : TypeToken<Map<String, Any>>() {}.type
                                gson.fromJson<Map<String, Any>>(dataElement, type)
                            } else null

                            Handler(Looper.getMainLooper()).post {
                                onCashStatusReceived?.invoke(data)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error procesando CASH_GET_STATUS: ${e.message}")
                            Handler(Looper.getMainLooper()).post {
                                onCashStatusReceived?.invoke(null)
                            }
                        }
                    }

                    "CASH_CLOSE" -> {
                        if (status && jsonObject.has("data")) {
                            val type = object : TypeToken<Map<String, Any>>() {}.type
                            val report: Map<String, Any> = gson.fromJson(jsonObject.get("data"), type)
                            Handler(Looper.getMainLooper()).post {
                                onCashCloseReportReceived?.invoke(report)
                                onOperationResult?.invoke(action, status, requestId)
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                onOperationResult?.invoke(action, status, message ?: requestId)
                            }
                        }
                    }

                    "CASH_GET_HISTORY" -> {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val response: Map<String, Any> = gson.fromJson(json, type)
                        val data = response["data"] as? List<Map<String, Any>> ?: emptyList()

                        Handler(Looper.getMainLooper()).post {
                            onCashHistoryReceived?.invoke(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en CashRepository: ${e.message}")
            }
        }
    }
    fun authorizeAndOpenCash(
        idEmpleado: Int,
        montoApertura: Double,
        superUser: String,
        superPass: String,
        requestId: String
    ) {
        val idSucursal = sessionManager.getSession()?.idSucursal ?: -1
        val params = mapOf(
            "id_empleado" to idEmpleado,
            "id_sucursal" to idSucursal,
            "monto_apertura" to montoApertura,
            "supervisor_user" to superUser,
            "supervisor_pass" to superPass
        )
        socketManager.sendAction("CASH_OPEN_AUTHORIZED", params, requestId)
    }
    fun openCash(idEmpleado: Int, montoApertura: Double, requestId: String) {
        val idSucursal = sessionManager.getSession()?.idSucursal ?: -1
        val params = mapOf(
            "id_empleado" to idEmpleado,
            "id_sucursal" to idSucursal,
            "monto_apertura" to montoApertura
        )
        socketManager.sendAction("CASH_OPEN", params, requestId)
    }
    fun getCashStatus(idEmpleado: Int) {
        val idSucursal = sessionManager.getSession()?.idSucursal ?: -1
        val params = mapOf(
            "id_empleado" to idEmpleado,
            "id_sucursal" to idSucursal
        )
        socketManager.sendAction("CASH_GET_STATUS", params)
    }
    fun addManualMovement(idSesion: Int, tipo: String, monto: Double, motivo: String, requestId: String) {
        val params = mapOf(
            "id_sesion" to idSesion,
            "tipo" to tipo, // 'INGRESO' o 'EGRESO'
            "monto" to monto,
            "motivo" to motivo
        )
        socketManager.sendAction("CASH_ADD_MOVEMENT", params, requestId)
    }
    fun closeCash(idSesion: Int, montoCierre: Double, requestId: String) {
        val params = mapOf(
            "id_sesion" to idSesion,
            "monto_cierre" to montoCierre
        )
        socketManager.sendAction("CASH_CLOSE", params, requestId)
    }

    fun getCashHistory(idSesion: Int, requestId: String) {
        val params = mapOf(
            "id_sesion" to idSesion
        )
        socketManager.sendAction("CASH_GET_HISTORY", params, requestId)
    }

    fun onSocketReconnected() {
        registerObserver()
    }

    fun clear() {
        socketManager.removeObserver(TAG)
        onOperationResult = null
        onCashStatusReceived = null
        onCashHistoryReceived = null
        onCashCloseReportReceived = null
    }
}