package com.kasolution.verify.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.UI.Employees.model.Empleado
import org.json.JSONObject

class EmpleadoRepository(private val socketManager: SocketManager) {

    private val TAG = "EmpleadoRepository"
    private val gson = Gson()

    var onEmpleadosListReceived: ((List<Empleado>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    /**
     * Registra (o re-registra) el observer del socket.
     * ESTA es la pieza clave para soportar reconexiones.
     */
    private fun registerObserver() {
        Log.d(TAG, "Registrando observer de EmpleadoRepository")

        // 🔴 Muy importante: eliminar antes para evitar observers zombies
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                val jsonObject = JSONObject(json)
                val action = jsonObject.optString("action")
                val status = jsonObject.optString("status") == "success"
                val requestId = jsonObject.optString("request_id", null)

                Log.d(TAG, "Repo interceptó acción: $action")

                when (action) {
                    "EMPLEADO_GET_ALL" -> {
                        val type = object :
                            TypeToken<SocketResponse<List<Empleado>>>() {}.type
                        val response: SocketResponse<List<Empleado>> =
                            gson.fromJson(json, type)

                        val lista = response.data ?: emptyList()

                        if (onEmpleadosListReceived == null) {
                            Log.e(
                                TAG,
                                "onEmpleadosListReceived es NULL (ViewModel no suscrito aún)"
                            )
                        }

                        onEmpleadosListReceived?.invoke(lista)
                    }

                    "EMPLEADO_SAVE",
                    "EMPLEADO_UPDATE",
                    "EMPLEADO_DELETE" -> {
                        Log.d(
                            TAG,
                            "Resultado operación $action → success=$status, requestId=$requestId"
                        )
                        onOperationResult?.invoke(action, status, requestId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico procesando mensaje", e)
            }
        }
    }

    /**
     * Se debe llamar cuando el socket se reconecta
     */
    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → re-registrando observer")
        registerObserver()
    }

    /* =========================
       ACCIONES
       ========================= */

    fun getEmpleados() {
        socketManager.sendAction("EMPLEADO_GET_ALL")
    }

    fun saveEmpleado(empleado: Empleado, pass: String, requestId: String) {
        val params = mapOf(
            "nombre" to empleado.nombre,
            "usuario" to empleado.usuario,
            "password" to pass,
            "rol" to empleado.rol.uppercase(),
            "estado" to if (empleado.estado) 1 else 0
        )
        socketManager.sendAction("EMPLEADO_SAVE", params, requestId)
    }

    fun updateEmpleado(empleado: Empleado, pass: String?, requestId: String) {
        val params = mutableMapOf(
            "id" to empleado.id.toString(),
            "nombre" to empleado.nombre,
            "usuario" to empleado.usuario,
            "rol" to empleado.rol,
            "estado" to empleado.estado.toString()
        )
        if (!pass.isNullOrEmpty()) {
            params["password"] = pass
        }
        socketManager.sendAction("EMPLEADO_UPDATE", params, requestId)
    }

    fun deleteEmpleado(id: Int, requestId: String) {
        socketManager.sendAction(
            "EMPLEADO_DELETE",
            mapOf("id" to id.toString()),
            requestId
        )
    }

    /**
     * Limpieza total (cuando el ViewModel muere)
     */
    fun clear() {
        Log.d(TAG, "Cerrando repositorio y removiendo observer")
        socketManager.removeObserver(TAG)
        onEmpleadosListReceived = null
        onOperationResult = null
    }
}
