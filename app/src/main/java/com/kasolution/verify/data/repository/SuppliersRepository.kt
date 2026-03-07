package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.remote.dto.SupplierDto

class SuppliersRepository(private val socketManager: SocketManager) {

    private val TAG = "SuppliersRepository"
    private val gson = Gson()

    var onSuppliersListReceived: ((List<Supplier>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    /**
     * Registra el repositorio en el SocketManager.
     * Se llama en el init y también desde el ViewModel para reactivar la escucha.
     */
    fun registerObserver() {
        Log.d(TAG, "Registrando observer de SuppliersRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                // 1. Parseo defensivo (Background thread)
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                // Lectura segura de la acción
                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                when (action) {
                    "SUPPLIER_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<SupplierDto>>>() {}.type
                        val response: SocketResponse<List<SupplierDto>> = gson.fromJson(json, type)

                        // Mapeo seguro de DTO a Dominio
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        Log.d(TAG, "Proveedores mapeados con éxito: ${listaDomain.size}")

                        // 2. Respuesta siempre en el hilo principal
                        Handler(Looper.getMainLooper()).post {
                            onSuppliersListReceived?.invoke(listaDomain)
                        }
                    }

                    "SUPPLIER_SAVE", "SUPPLIER_UPDATE", "SUPPLIER_DELETE" -> {
                        val status = if (jsonObject.has("status")) {
                            jsonObject.get("status").asString == "success"
                        } else false

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
                Log.e(TAG, "Error procesando mensaje en $TAG: ${e.message}")
            }
        }
    }

    /* ============================
       PETICIONES AL SERVIDOR
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
            "id_proveedor" to supplier.id, // Enviamos el Int directamente
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
            mapOf("id_proveedor" to id),
            requestId
        )
    }

    /* ============================
       RECONEXIÓN Y LIMPIEZA
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → Refrescando proveedores")
        registerObserver()
        getSuppliers()
    }

    fun clear() {
        Log.d(TAG, "Cerrando SuppliersRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onSuppliersListReceived = null
        onOperationResult = null
    }
}