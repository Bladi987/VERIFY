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
import com.kasolution.verify.data.remote.dto.RoleDto
import com.kasolution.verify.domain.role.model.Role


class RolRepository(private val socketManager: SocketManager) {

    private val TAG = "RolRepository"
    private val gson = Gson()

    var onRolesListReceived: ((List<Role>) -> Unit)? = null
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

                val action = if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull)
                    jsonObject.get("action").asString else ""

                val status = if (jsonObject.has("status"))
                    jsonObject.get("status").asString == "success" else false

                val requestId =
                    if (jsonObject.has("request_id") && !jsonObject.get("request_id").isJsonNull)
                        jsonObject.get("request_id").asString else null

                when (action) {
                    "ROLE_GET_ALL", "ROL_GET_ALL" -> {
                        val type = object : TypeToken<SocketResponse<List<RoleDto>>>() {}.type
                        val response: SocketResponse<List<RoleDto>> = gson.fromJson(json, type)
                        val listaDomain = response.data?.map { it.toDomain() } ?: emptyList()

                        Log.d(TAG, "Roles mapeados correctamente: ${listaDomain.size}")

                        Handler(Looper.getMainLooper()).post {
                            onRolesListReceived?.invoke(listaDomain)
                        }
                    }

                    "ROLE_SAVE", "ROLE_UPDATE", "ROLE_DELETE",
                    "ROL_SAVE", "ROL_UPDATE", "ROL_DELETE" -> {
                        Handler(Looper.getMainLooper()).post {
                            onOperationResult?.invoke(action, status, requestId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en parseo Rol: ${e.message}")
            }
        }
    }

    /* ============================
       ACCIONES CRUD SIMPLIFICADAS
       ============================ */

    fun getRoles() {
        socketManager.sendAction("ROLE_GET_ALL")
    }

    fun saveRole(role: Role, requestId: String) {
        val params = mapOf<String, Any>(
            "nombre_rol" to role.nombre,
            "slug_rol" to role.slug,
            "descripcion" to (role.descripcion ?: ""),
            "estado" to if (role.estado) 1 else 0
        )

        socketManager.sendAction("ROLE_SAVE", params, requestId)
    }

    fun updateRole(role: Role, requestId: String) {
        val params = mapOf<String, Any>(
            "id_rol" to role.id,
            "nombre_rol" to role.nombre,
            "slug_rol" to role.slug,
            "descripcion" to (role.descripcion ?: ""),
            "estado" to if (role.estado) 1 else 0
        )

        socketManager.sendAction("ROLE_UPDATE", params, requestId)
    }

    fun deleteRole(id: Int, requestId: String) {
        socketManager.sendAction(
            "ROLE_DELETE",
            mapOf("id_rol" to id),
            requestId
        )
    }

    fun onSocketReconnected() {
        getRoles()
    }

    fun clear() {
        socketManager.removeObserver(TAG)
        onRolesListReceived = null
        onOperationResult = null
    }

}