package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.remote.dto.ClientDto

class ClientsRepository(private val socketManager: SocketManager) {

    private val TAG = "ClientsRepository"
    private val gson = Gson()

    // Callbacks para el ViewModel
    var onClientsListReceived: ((List<Client>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        socketManager.removeObserver(TAG)
        socketManager.addObserver(TAG) { json ->
            try {
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                // Lectura segura de la acción para evitar JsonNull
                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                when (action) {
                    "CLIENTE_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<ClientDto>>>() {}.type
                        val response: SocketResponse<List<ClientDto>> = gson.fromJson(json, type)

                        // Mapeo seguro de DTO a Dominio
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        Log.d(TAG, "Clientes mapeados con éxito: ${listaDomain.size}")

                        // Respuesta siempre en el hilo principal
                        Handler(Looper.getMainLooper()).post {
                            onClientsListReceived?.invoke(listaDomain)
                        }
                    }

                    "CLIENTE_SAVE", "CLIENTE_UPDATE", "CLIENTE_DELETE" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        val requestId = if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull) {
                            jsonObject.get("request_id").asString
                        } else null

                        Log.d(TAG, "Resultado $action → success=$status requestId=$requestId")

                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico procesando mensaje en $TAG: ${e.message}")
            }
        }
    }

    /* ============================
       PETICIONES AL SERVIDOR
       ============================ */

    fun getClients() {
        socketManager.sendAction("CLIENTE_GET_ALL")
    }

    fun saveClient(cliente: Client, requestId: String) {
        val params = mapOf(
            "nombre" to cliente.nombre,
            "dni_ruc" to cliente.dniRuc,
            "telefono" to cliente.telefono,
            "email" to cliente.email,
            "direccion" to cliente.direccion
        )
        socketManager.sendAction("CLIENTE_SAVE", params, requestId)
    }

    fun updateClient(cliente: Client, requestId: String) {
        val params = mapOf(
            "id_cliente" to cliente.id,
            "nombre" to cliente.nombre,
            "dni_ruc" to cliente.dniRuc,
            "telefono" to cliente.telefono,
            "email" to cliente.email,
            "direccion" to cliente.direccion
        )
        socketManager.sendAction("CLIENTE_UPDATE", params, requestId)
    }

    fun deleteClient(id: Int, requestId: String) {
        socketManager.sendAction(
            "CLIENTE_DELETE",
            mapOf("id_cliente" to id),
            requestId
        )
    }

    /* ============================
       GESTIÓN DE ESTADO Y LIMPIEZA
       ============================ */

    /**
     * Función vital para recuperar datos automáticamente tras una caída de red
     */
    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → Solicitando actualización de clientes")
        getClients()
    }

    /**
     * Limpia observadores y callbacks para evitar fugas de memoria (Memory Leaks)
     */
    fun clear() {
        Log.d(TAG, "Cerrando ClientsRepository y removiendo observer de SocketManager")
        socketManager.removeObserver(TAG)
        onClientsListReceived = null
        onOperationResult = null
    }
}