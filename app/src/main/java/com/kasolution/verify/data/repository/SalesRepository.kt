package com.kasolution.verify.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import org.json.JSONObject

class SalesRepository(private val socketManager: SocketManager){

    private val TAG = "ClientsRepository"
    private val gson = Gson()

    var onClientsListReceived: ((List<Cliente>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        socketManager.addObserver(TAG) { json ->
            try {
                val jsonObject = JSONObject(json)
                val action = jsonObject.optString("action")
                val status = jsonObject.optString("status") == "success"
                val requestId = jsonObject.optString("request_id", null)

                Log.d(TAG, "Repo interceptó acción: $action")

                when (action) {

                    "CLIENTE_GET_ALL" -> {
                        val type =
                            object : TypeToken<SocketResponse<List<Cliente>>>() {}.type
                        val response: SocketResponse<List<Cliente>> =
                            gson.fromJson(json, type)

                        val lista = response.data ?: emptyList()

                        if (onClientsListReceived == null) {
                            Log.e(
                                TAG,
                                "onClientsListReceived es NULL (ViewModel no suscrito aún)"
                            )
                        }

                        onClientsListReceived?.invoke(lista)
                    }

                    "CLIENTE_SAVE",
                    "CLIENTE_UPDATE",
                    "CLIENTE_DELETE" -> {
                        Log.d(TAG, "Resultado $action → success=$status requestId=$requestId")
                        onOperationResult?.invoke(action, status, requestId)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error procesando mensaje socket: ${e.message}", e)
            }
        }
    }

    /* ============================
       PETICIONES
       ============================ */

    fun getClients() {
        socketManager.sendAction("CLIENTE_GET_ALL")
    }

    /* ============================
       RECONEXIÓN
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → solicitando CLIENTE_GET_ALL")
        getClients()
    }

    /* ============================
       LIMPIEZA (NO USAR EN onCleared)
       ============================ */

    fun clear() {
        Log.d(TAG, "Cerrando ClientsRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onClientsListReceived = null
        onOperationResult = null
    }
}
