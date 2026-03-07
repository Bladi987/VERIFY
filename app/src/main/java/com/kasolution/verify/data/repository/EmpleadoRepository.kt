package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.remote.dto.EmployeeDto
import com.kasolution.verify.domain.employees.model.Employee

class EmpleadoRepository(private val socketManager: SocketManager) {

    private val TAG = "EmpleadoRepository"
    private val gson = Gson()

    var onEmpleadosListReceived: ((List<Employee>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    /**
     * Registra el repositorio en el SocketManager.
     * Ahora es pública para que el ViewModel la reactive al entrar a la actividad.
     */
    fun registerObserver() {
        Log.d(TAG, "Registrando observer de EmpleadoRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                // 1. Parseo defensivo (Background thread)
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                // Lectura segura de campos
                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                val status = if (jsonObject.has("status")) {
                    jsonObject.get("status").asString == "success"
                } else false

                val requestId = if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull) {
                    jsonObject.get("request_id").asString
                } else null

                Log.d(TAG, "Repo interceptó acción: $action")

                when (action) {
                    "EMPLEADO_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<EmployeeDto>>>() {}.type
                        val response: SocketResponse<List<EmployeeDto>> = gson.fromJson(json, type)

                        val listaDto = response.data ?: emptyList()
                        val listaDomain = listaDto.map { it.toDomain() }

                        Log.d(TAG, "Empleados mapeados correctamente: ${listaDomain.size}")

                        // 2. Respuesta en el hilo principal
                        Handler(Looper.getMainLooper()).post {
                            onEmpleadosListReceived?.invoke(listaDomain)
                        }
                    }

                    "EMPLEADO_SAVE",
                    "EMPLEADO_UPDATE",
                    "EMPLEADO_DELETE" -> {
                        Log.d(TAG, "Resultado operación $action → success=$status, requestId=$requestId")

                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico procesando mensaje en EmpleadoRepository", e)
            }
        }
    }

    /* =========================
       ACCIONES
       ========================= */

    fun getEmpleados() {
        socketManager.sendAction("EMPLEADO_GET_ALL")
    }

    fun saveEmpleado(empleado: Employee, pass: String, requestId: String) {
        val params = mapOf(
            "nombre" to empleado.nombre,
            "usuario" to empleado.usuario,
            "password" to pass,
            "rol" to empleado.rol.uppercase(),
            "estado" to if (empleado.estado) 1 else 0
        )
        socketManager.sendAction("EMPLEADO_SAVE", params, requestId)
    }

    fun updateEmpleado(empleado: Employee, pass: String?, requestId: String) {
        // Usamos tipos nativos (Int/Boolean) en lugar de toString() manual
        val params = mutableMapOf<String, Any>(
            "id" to empleado.id,
            "nombre" to empleado.nombre,
            "usuario" to empleado.usuario,
            "rol" to empleado.rol,
            "estado" to if (empleado.estado) 1 else 0
        )
        if (!pass.isNullOrEmpty()) {
            params["password"] = pass
        }
        socketManager.sendAction("EMPLEADO_UPDATE", params, requestId)
    }

    fun deleteEmpleado(id: Int, requestId: String) {
        socketManager.sendAction(
            "EMPLEADO_DELETE",
            mapOf("id" to id),
            requestId
        )
    }

    /* =========================
       GESTIÓN DE ESTADO Y LIMPIEZA
       ========================= */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → Refrescando empleados")
        // No solo re-registramos, pedimos los datos
        registerObserver()
        getEmpleados()
    }

    fun clear() {
        Log.d(TAG, "Cerrando repositorio de Empleados y removiendo observer")
        socketManager.removeObserver(TAG)
        onEmpleadosListReceived = null
        onOperationResult = null
    }
}