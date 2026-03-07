package com.kasolution.verify.data.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.mapper.toDomain
import com.kasolution.verify.data.remote.dto.CategoryDto
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager

class CategoriesRepository(private val socketManager: SocketManager) {

    private val TAG = "CategoriesRepository"
    private val gson = Gson()

    // Callbacks para el ViewModel
    var onCategoriesListReceived: ((List<Category>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        socketManager.removeObserver(TAG)
        socketManager.addObserver(TAG) { json ->
            try {
                // 1. Parseo pesado ocurre en hilo de fondo (Background)
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull)
                    jsonObject.get("action").asString else ""

                when (action) {
                    "CATEGORY_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<CategoryDto>>>() {}.type
                        val response: SocketResponse<List<CategoryDto>> = gson.fromJson(json, type)
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        // 2. SOLO volvemos al hilo principal para actualizar la UI
                        Handler(Looper.getMainLooper()).post {
                            onCategoriesListReceived?.invoke(listaDomain)
                        }
                    }
                    "CATEGORY_SAVE", "CATEGORY_UPDATE", "CATEGORY_DELETE" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        val requestId = jsonObject.get("request_id")?.asString

                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo: ${e.message}")
            }
        }
    }

    /* ============================
       ACCIONES (Simplificadas con el nuevo SocketManager)
       ============================ */

    fun getCategories() {
        socketManager.sendAction("CATEGORY_GET_ALL")
    }

    fun saveCategory(category: Category, requestId: String) {
        // El SocketManager ahora escapa caracteres automáticamente
        val params = mapOf(
            "nombre" to category.nombre,
            "descripcion" to (category.descripcion ?: ""),
            "estado" to if (category.estado) 1 else 0 // Enviamos 1/0 para DB
        )
        socketManager.sendAction("CATEGORY_SAVE", params, requestId)
    }

    fun updateCategory(category: Category, requestId: String) {
        val params = mapOf(
            "id_categoria" to category.id, // Pasamos el Int directamente
            "nombre" to category.nombre,
            "descripcion" to (category.descripcion ?: ""),
            "estado" to if (category.estado) 1 else 0
        )
        socketManager.sendAction("CATEGORY_UPDATE", params, requestId)
    }

    fun deleteCategory(id: Int, requestId: String) {
        socketManager.sendAction(
            "CATEGORY_DELETE",
            mapOf("id_categoria" to id),
            requestId
        )
    }

    fun onSocketReconnected() {
        // Al reconectar, no necesitamos volver a registrar el observer,
        // solo pedir los datos de nuevo.
        getCategories()
    }

    // Limpieza vital para evitar fugas de memoria
    fun clear() {
        Log.d(TAG, "Dando de baja observador de Categorías")
        socketManager.removeObserver(TAG)
        onCategoriesListReceived = null
        onOperationResult = null
    }
}