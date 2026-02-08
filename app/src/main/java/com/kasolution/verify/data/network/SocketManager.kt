package com.kasolution.verify.data.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kasolution.verify.data.local.SessionManager
import okhttp3.*
import java.util.concurrent.TimeUnit

class SocketManager private constructor() {

    val TAG = "SocketManager"

    private var client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null

    private var sessionManager: SessionManager? = null

    var isConnected: Boolean = false
        private set

    // Callbacks
    var onConnected: (() -> Unit)? = null
    var onConnectionError: ((String) -> Unit)? = null

    private val observers = mutableMapOf<String, (String) -> Unit>()

    companion object {
        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(): SocketManager =
            instance ?: synchronized(this) {
                instance ?: SocketManager().also { instance = it }
            }
    }

    // 🔗 Se llama UNA sola vez (por ejemplo en Application o Login)
    fun bindSessionManager(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    // --- OBSERVADORES ---
    fun addObserver(tag: String, callback: (String) -> Unit) {
        observers.remove(tag)
        observers[tag] = callback
        Log.d(TAG, "NUEVO REGISTRO: $tag. Total ahora: ${observers.size}")
    }

    fun removeObserver(tag: String) {
        observers.remove(tag)
    }

    fun unregisterConnectionListener() {
        onConnected = null
    }

    // --- CONEXIÓN ---
    fun connect(url: String) {

        // 🔐 BONUS: no conectar si no hay sesión
        if (sessionManager?.isUserLoggedIn() == false) {
            Log.w(TAG, "Intento de conexión sin sesión activa. Abortando.")
            return
        }

        if (webSocket != null && currentUrl != url) {
            Log.d(TAG, "Cambio de IP detectado. Cerrando conexión vieja...")
            close()
        } else if (isConnected) {
            Log.d(TAG, "Ya conectado a $url")
            return
        }

        currentUrl = url
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "Conectado al servidor Ratchet")
                Handler(Looper.getMainLooper()).post {
                    onConnected?.invoke()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Mensaje recibido: $text")
                    observers.values.forEach { it.invoke(text) }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                this@SocketManager.webSocket = null
                Log.e(TAG, "Error de conexión: ${t.message}")

                Handler(Looper.getMainLooper()).post {
                    onConnectionError?.invoke(t.message ?: "Error desconocido")
                }

                retryConnection()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                this@SocketManager.webSocket = null
                Log.d(TAG, "🔌 Conexión cerrada.")
            }
        })
    }

    private fun retryConnection() {

        // 🔐 BONUS: no reintentar si no hay sesión
        if (sessionManager?.isUserLoggedIn() == false) {
            Log.w(TAG, "Reconexión cancelada: sesión cerrada.")
            return
        }

        currentUrl?.let { url ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isConnected && webSocket == null) {
                    connect(url)
                }
            }, 3000)
        }
    }

    // --- ACCIONES ---
    fun sendAction(
        action: String,
        params: Map<String, Any>? = null,
        requestId: String? = null
    ) {
        if (!isConnected) {
            Log.e(TAG, "Envío fallido: Socket desconectado ($action)")
            return
        }

        val json = StringBuilder()
        json.append("{ \"action\": \"$action\"")

        if (requestId != null) {
            json.append(", \"request_id\": \"$requestId\"")
        }

        params?.forEach { (key, value) ->
            json.append(", \"$key\": \"$value\"")
        }

        json.append(" }")
        webSocket?.send(json.toString())
    }

    fun close() {
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        isConnected = false
        onConnected = null
    }

    // --- TEST DE CONEXIÓN ---
    fun testConnection(url: String, onResult: (Boolean, String) -> Unit) {
        val testClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        testClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.close(1000, "Test finalizado")
                onResult(true, "OK")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onResult(false, t.message ?: "Servidor no alcanzado")
            }
        })
    }
}
