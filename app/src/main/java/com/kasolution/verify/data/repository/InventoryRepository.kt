package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.remote.dto.ProductDto

class InventoryRepository(private val socketManager: SocketManager) {

    private val TAG = "InventoryRepository"
    private val gson = Gson()

    var onInventoryListReceived: ((List<Product>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    /**
     * Registra el repositorio en el SocketManager.
     * Se llama en el init y también desde el ViewModel para reactivar la escucha.
     */
    fun registerObserver() {
        Log.d(TAG, "Registrando observer de InventoryRepository")
        socketManager.removeObserver(TAG)

        socketManager.addObserver(TAG) { json ->
            try {
                // 1. Parseo defensivo con JsonParser (Hilo de fondo del Socket)
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                // Lectura segura de la acción
                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                when (action) {
                    "PRODUCT_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<ProductDto>>>() {}.type
                        val response: SocketResponse<List<ProductDto>> = gson.fromJson(json, type)

                        // Mapeo masivo a dominio (operación costosa en memoria)
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        Log.d(TAG, "Productos mapeados con éxito: ${listaDomain.size}")

                        // 2. IMPORTANTE: Entregar el resultado en el hilo principal
                        Handler(Looper.getMainLooper()).post {
                            onInventoryListReceived?.invoke(listaDomain)
                        }
                    }

                    "PRODUCT_SAVE", "PRODUCT_UPDATE", "PRODUCT_DELETE" -> {
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

    fun getProducts() {
        socketManager.sendAction("PRODUCT_GET_ALL")
    }

    fun saveProduct(product: Product, requestId: String) {
        val params = mapOf(
            "codigo" to product.codigo,
            "nombre" to product.nombre,
            "id_categoria" to product.idCategoria,
            "id_proveedor" to product.idProveedor,
            "precio_compra" to product.precioCompra,
            "precio_venta" to product.precioVenta,
            "stock" to product.stock,
            "unidad_medida" to product.unidadMedida,
            "estado" to if (product.estado) 1 else 0 // Aseguramos formato numérico para el server
        )
        socketManager.sendAction("PRODUCT_SAVE", params, requestId)
    }

    fun updateProduct(product: Product, requestId: String) {
        val params = mapOf(
            "id_producto" to product.id, // Enviamos el Int directamente
            "codigo" to product.codigo,
            "nombre" to product.nombre,
            "id_categoria" to product.idCategoria,
            "id_proveedor" to product.idProveedor,
            "precio_compra" to product.precioCompra,
            "precio_venta" to product.precioVenta,
            "stock" to product.stock,
            "unidad_medida" to product.unidadMedida,
            "estado" to if (product.estado) 1 else 0
        )
        socketManager.sendAction("PRODUCT_UPDATE", params, requestId)
    }

    fun deleteProduct(id: Int, requestId: String) {
        socketManager.sendAction(
            "PRODUCT_DELETE",
            mapOf("id_producto" to id),
            requestId
        )
    }

    /* ============================
       RECONEXIÓN Y LIMPIEZA
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → Refrescando inventario")
        registerObserver()
        getProducts()
    }

    fun clear() {
        Log.d(TAG, "Cerrando InventoryRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onInventoryListReceived = null
        onOperationResult = null
    }
}