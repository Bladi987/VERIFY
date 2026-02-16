package com.kasolution.verify.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import org.json.JSONObject

class InventoryRepository(private val socketManager: SocketManager){

    private val TAG = "InventoryRepository"
    private val gson = Gson()

    var onInventoryListReceived: ((List<Product>) -> Unit)? = null
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

                    "PRODUCT_GET_ALL" -> {
                        val type =
                            object : TypeToken<SocketResponse<List<Product>>>() {}.type
                        val response: SocketResponse<List<Product>> =
                            gson.fromJson(json, type)

                        val lista = response.data ?: emptyList()

                        if (onInventoryListReceived == null) {
                            Log.e(
                                TAG,
                                "onInventoryListReceived es NULL (ViewModel no suscrito aún)"
                            )
                        }

                        onInventoryListReceived?.invoke(lista)
                    }

                    "PRODUCT_SAVE",
                    "PRODUCT_UPDATE",
                    "PRODUCT_DELETE" -> {
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
            "estado" to product.estado
        )
        socketManager.sendAction("PRODUCT_SAVE", params, requestId)
    }

    fun updateProduct(product: Product, requestId: String) {
        val params = mapOf(
            "id_producto" to product.id.toString(),
            "codigo" to product.codigo,
            "nombre" to product.nombre,
            "id_categoria" to product.idCategoria,
            "id_proveedor" to product.idProveedor,
            "precio_compra" to product.precioCompra,
            "precio_venta" to product.precioVenta,
            "stock" to product.stock,
            "unidad_medida" to product.unidadMedida,
            "estado" to product.estado
        )
        socketManager.sendAction("PRODUCT_UPDATE", params, requestId)
    }

    fun deleteProduct(id: Int, requestId: String) {
        socketManager.sendAction(
            "PRODUCT_DELETE",
            mapOf("id_producto" to id.toString()),
            requestId
        )
    }

    /* ============================
       RECONEXIÓN
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → solicitando PRODUCTS_GET_ALL")
        getProducts()
    }

    /* ============================
       LIMPIEZA (NO USAR EN onCleared)
       ============================ */

    fun clear() {
        Log.d(TAG, "Cerrando InventoryRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onInventoryListReceived = null
        onOperationResult = null
    }
}
