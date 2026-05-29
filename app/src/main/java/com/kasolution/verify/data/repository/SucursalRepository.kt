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
import com.kasolution.verify.data.remote.dto.BranchDto
import com.kasolution.verify.domain.branch.model.Branch

class SucursalRepository(private val socketManager: SocketManager) {

    private val TAG = "SucursalRepository"
    private val gson = Gson()

    // Callbacks para el ViewModel (Mismo estilo que Categories)
    var onBranchesListReceived: ((List<Branch>) -> Unit)? = null
    var onOperationResult: ((String, Boolean, String?) -> Unit)? = null

    init {
        registerObserver()
    }

    fun registerObserver() {
        socketManager.removeObserver(TAG)
        socketManager.addObserver(TAG) { json ->
            try {
                // 1. Parseo defensivo en hilo de fondo
                val element = JsonParser.parseString(json)
                if (!element.isJsonObject) return@addObserver
                val jsonObject = element.asJsonObject

                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull)
                    jsonObject.get("action").asString else ""

                when (action) {
                    "BRANCH_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<BranchDto>>>() {}.type
                        val response: SocketResponse<List<BranchDto>> = gson.fromJson(json, type)
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        Handler(Looper.getMainLooper()).post {
                            onBranchesListReceived?.invoke(listaDomain)
                        }
                    }
                    "BRANCH_SAVE", "BRANCH_UPDATE", "BRANCH_DELETE" -> {
                        val status = jsonObject.get("status")?.asString == "success"
                        val requestId = jsonObject.get("request_id")?.asString

                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo Sucursal: ${e.message}")
            }
        }
    }

    /* ============================
       ACCIONES
       ============================ */

    fun getBranches() {
        socketManager.sendAction("BRANCH_GET_ALL")
    }

    fun saveBranch(branch: Branch, requestId: String) {
        val params = mapOf(
            "nombre" to branch.nombre,
            "ruc" to (branch.ruc ?: ""),
            "direccion" to (branch.direccion ?: ""),
            "telefono" to (branch.telefono ?: ""),
            "estado" to if (branch.estado) 1 else 0
        )
        socketManager.sendAction("BRANCH_SAVE", params, requestId)
    }

    fun updateBranch(branch: Branch, requestId: String) {
        val params = mapOf(
            "id_sucursal" to branch.id,
            "nombre" to branch.nombre,
            "ruc" to (branch.ruc ?: ""),
            "direccion" to (branch.direccion ?: ""),
            "telefono" to (branch.telefono ?: ""),
            "estado" to if (branch.estado) 1 else 0
        )
        socketManager.sendAction("BRANCH_UPDATE", params, requestId)
    }

    fun deleteBranch(id: Int, requestId: String) {
        socketManager.sendAction(
            "BRANCH_DELETE",
            mapOf("id_sucursal" to id),
            requestId
        )
    }

    fun onSocketReconnected() {
        getBranches()
    }

    fun clear() {
        Log.d(TAG, "Dando de baja observador de Sucursales")
        socketManager.removeObserver(TAG)
        onBranchesListReceived = null
        onOperationResult = null
    }
}