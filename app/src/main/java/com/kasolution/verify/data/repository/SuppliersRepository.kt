package com.kasolution.verify.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.UI.Suppliers.model.Supplier
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import org.json.JSONObject

class SuppliersRepository(private val socketManager: SocketManager){

    private val TAG = "SuppliersRepository"
    private val gson = Gson()

    var onSuppliersListReceived: ((List<Supplier>) -> Unit)? = null
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

                    "SUPPLIER_GET_ALL" -> {
                        val type =
                            object : TypeToken<SocketResponse<List<Supplier>>>() {}.type
                        val response: SocketResponse<List<Supplier>> =
                            gson.fromJson(json, type)

                        val lista = response.data ?: emptyList()

                        if (onSuppliersListReceived == null) {
                            Log.e(
                                TAG,
                                "onSuppliersListReceived es NULL (ViewModel no suscrito aún)"
                            )
                        }

                        onSuppliersListReceived?.invoke(lista)
                    }

                    "SUPPLIER_SAVE",
                    "SUPPLIER_UPDATE",
                    "SUPPLIER_DELETE" -> {
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

    fun getSuppliers() {
        socketManager.sendAction("SUPPLIER_GET_ALL")
    }

    fun saveSupplier(supplier: Supplier, requestId: String) {
        val params = mapOf(
            "nombre" to supplier.nombre,
            "telefono" to supplier.telefono,
            "email" to supplier.email,
            "direccion" to supplier.direccion
        )
        socketManager.sendAction("SUPPLIER_SAVE", params, requestId)
    }

    fun updateSupplier(supplier: Supplier, requestId: String) {
        val params = mapOf(
            "id_proveedor" to supplier.id.toString(),
            "nombre" to supplier.nombre,
            "telefono" to supplier.telefono,
            "email" to supplier.email,
            "direccion" to supplier.direccion
        )
        socketManager.sendAction("SUPPLIER_UPDATE", params, requestId)
    }

    fun deleteSupplier(id: Int, requestId: String) {
        socketManager.sendAction(
            "SUPPLIER_DELETE",
            mapOf("id_proveedor" to id.toString()),
            requestId
        )
    }

    /* ============================
       RECONEXIÓN
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → solicitando SUPPLIER_GET_ALL")
        getSuppliers()
    }

    /* ============================
       LIMPIEZA (NO USAR EN onCleared)
       ============================ */

    fun clear() {
        Log.d(TAG, "Cerrando SuppliersRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onSuppliersListReceived = null
        onOperationResult = null
    }
}
