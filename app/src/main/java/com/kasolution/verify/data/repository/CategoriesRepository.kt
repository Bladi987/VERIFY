package com.kasolution.verify.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.data.model.SocketResponse
import com.kasolution.verify.data.network.SocketManager
import org.json.JSONObject

class CategoriesRepository(private val socketManager: SocketManager){

    private val TAG = "CategoriesRepository"
    private val gson = Gson()

    var onCategoriesListReceived: ((List<Category>) -> Unit)? = null
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

                    "CATEGORY_GET_ALL" -> {
                        val type =
                            object : TypeToken<SocketResponse<List<Category>>>() {}.type
                        val response: SocketResponse<List<Category>> =
                            gson.fromJson(json, type)

                        val lista = response.data ?: emptyList()

                        if (onCategoriesListReceived == null) {
                            Log.e(
                                TAG,
                                "onCategoriesListReceived es NULL (ViewModel no suscrito aún)"
                            )
                        }

                        onCategoriesListReceived?.invoke(lista)
                    }

                    "CATEGORY_SAVE",
                    "CATEGORY_UPDATE",
                    "CATEGORY_DELETE" -> {
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

    fun getCategories() {
        socketManager.sendAction("CATEGORY_GET_ALL")
    }

    fun saveCategory(category: Category, requestId: String) {
        val params = mapOf(
            "nombre" to category.nombre,
            "descripcion" to (category.descripcion ?: ""),
            "estado" to category.estado
        )
        socketManager.sendAction("CATEGORY_SAVE", params, requestId)
    }

    fun updateCategory(category: Category, requestId: String) {
        val params = mapOf(
            "id_categoria" to category.id.toString(),
            "nombre" to category.nombre,
            "descripcion" to (category.descripcion ?: ""),
            "estado" to category.estado
        )
        socketManager.sendAction("CATEGORY_UPDATE", params, requestId)
    }

    fun deleteCategory(id: Int, requestId: String) {
        socketManager.sendAction(
            "CATEGORY_DELETE",
            mapOf("id_categoria" to id.toString()),
            requestId
        )
    }

    /* ============================
       RECONEXIÓN
       ============================ */

    fun onSocketReconnected() {
        Log.d(TAG, "Socket reconectado → solicitando CATEGORY_GET_ALL")
        getCategories()
    }

    /* ============================
       LIMPIEZA (NO USAR EN onCleared)
       ============================ */

    fun clear() {
        Log.d(TAG, "Cerrando CategoriesRepository y removiendo observer")
        socketManager.removeObserver(TAG)
        onCategoriesListReceived = null
        onOperationResult = null
    }
}
