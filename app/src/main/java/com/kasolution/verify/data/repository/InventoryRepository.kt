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
    private var ultimaSucursalConsultada: Int = 0
    private var mode: String = "SALE"

    // Callbacks para el ViewModel (Estructura idéntica a Clientes)
    var onProductsListReceived: ((List<Product>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        socketManager.addObserver(TAG) { json ->
            try {
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                // Lectura de acción igual a Clientes
                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull) {
                    jsonObject.get("action").asString
                } else ""

                when (action) {
                    "PRODUCT_GET_ALL" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        if (status) {
                            val type =
                                object : TypeToken<SocketResponse<List<ProductDto>>>() {}.type
                            val response: SocketResponse<List<ProductDto>> =
                                gson.fromJson(json, type)
                            val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()
                            Handler(Looper.getMainLooper()).post {
                                onProductsListReceived?.invoke(listaDomain)
                            }
                        } else {
                            val message = jsonObject.get("message")?.asString
                                ?: "Error desconocido al recuperar catálogo."
                            Handler(Looper.getMainLooper()).post {
                                onOperationResult?.invoke(action, false, message)
                            }
                        }
                    }
                    "PRODUCT_GET_BY_CODE" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        if (status) {
                            val type = object : TypeToken<SocketResponse<ProductDto>>() {}.type
                            val response: SocketResponse<ProductDto> =
                                gson.fromJson(json, type)
                            val productDomain = response.data?.toDomain()
                            Handler(Looper.getMainLooper()).post {
                                onProductsListReceived?.invoke(listOfNotNull(productDomain))
                            }
                        } else {
                            val message = jsonObject.get("message")?.asString
                                ?: "Error desconocido al recuperar catálogo."
                            Handler(Looper.getMainLooper()).post {
                                onOperationResult?.invoke(action, false, null)
                            }
                        }
                    }

                    "PRODUCT_SAVE", "PRODUCT_UPDATE", "PRODUCT_DELETE" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        val requestId =
                            if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull) {
                                jsonObject.get("request_id").asString
                            } else null
                        val errorMessage = if (!status && jsonObject.has("message")) {
                            jsonObject.get("message").asString
                        } else null
                        Log.d(
                            TAG,
                            "Resultado $action → success=$status requestId=$requestId error=$errorMessage"
                        )

                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico procesando mensaje en $TAG: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onOperationResult?.invoke("ERROR_DATOS", false, null)
                }
            }
        }
    }

    /* ============================
       PETICIONES AL SERVIDOR
       ============================ */

    fun getProducts(idSucursal: Int,modo: String) {
        mode=modo
        if (idSucursal <= 0) return
        this.ultimaSucursalConsultada = idSucursal
        val params = mapOf("id_sucursal" to idSucursal,"modo" to mode)
        socketManager.sendAction("PRODUCT_GET_ALL", params)
    }
    fun getProductByCode(code: String, idSucursal: Int, modo:String,requestId: String) {
        if (idSucursal <= 0 || code.isBlank()) return
        this.ultimaSucursalConsultada = idSucursal
        val params = mapOf("id_sucursal" to idSucursal, "codigo" to code,"modo" to modo)
        socketManager.sendAction("PRODUCT_GET_BY_CODE", params,requestId)

    }

    fun saveProduct(product: Product,idSucursal: Int, requestId: String) {
        val params = mapOf(
            "id_sucursal" to idSucursal,
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
        socketManager.sendAction("PRODUCT_SAVE", params, requestId)
    }

    fun updateProduct(product: Product, requestId: String) {
        val params = mapOf(
            "id_producto" to product.id, // Coincide con InventoryController.php
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
            mapOf("id_producto" to id), // Envía el ID para el borrado lógico (estado = 0)
            requestId
        )
    }

    /* ============================
       GESTIÓN DE ESTADO Y LIMPIEZA
       ============================ */

    fun onSocketReconnected() {
        if (ultimaSucursalConsultada > 0) {
            Log.d(TAG, "Socket reconectado → Solicitando actualización automática para sucursal $ultimaSucursalConsultada")
            getProducts(ultimaSucursalConsultada,mode)
        } else {
            Log.w(TAG, "Socket reconectado pero no hay registro de sucursal previa para auto-refrescar.")
        }
    }

    fun clear() {
        Log.d(TAG, "Cerrando InventoryRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onProductsListReceived = null
        onOperationResult = null
    }
}