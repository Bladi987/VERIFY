package com.kasolution.verify.data.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.kasolution.verify.data.local.SessionManager
import okhttp3.*
import java.util.concurrent.TimeUnit

class SocketManager private constructor() {

    private val TAG = "SocketManager"
    private val gson = Gson() // Usaremos Gson para enviar JSONs válidos siempre

    private var client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var sessionManager: SessionManager? = null

    var isConnected: Boolean = false
        private set

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

    fun bindSessionManager(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    fun addObserver(tag: String, callback: (String) -> Unit) {
        observers[tag] = callback
    }

    fun removeObserver(tag: String) {
        observers.remove(tag)
    }

    fun connect(url: String) {
        if (sessionManager?.isUserLoggedIn() == false) return

        if (webSocket != null && currentUrl != url) {
            close()
        } else if (isConnected) return

        currentUrl = url
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Handler(Looper.getMainLooper()).post { onConnected?.invoke() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // IMPORTANTE: El parseo de JSON grandes debe ser fuera del hilo principal
                // pero como tus repositorios usan postValue, esto está bien por ahora.
                Log.d(TAG, "Mensaje recibido (Longitud: ${text.length})")

                // Si el mensaje es muy largo, Log.d lo corta en la consola,
                // pero la variable 'text' está completa.

                observers.values.forEach { it.invoke(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                this@SocketManager.webSocket = null
                Handler(Looper.getMainLooper()).post {
                    onConnectionError?.invoke(t.message ?: "Error de conexión")
                }
                retryConnection()
            }
        })
    }

    // --- ACCIONES (Corregido: Ahora usa un Map real para evitar JSONs mal formados) ---
    fun sendAction(
        action: String,
        params: Map<String, Any>? = null,
        requestId: String? = null
    ) {
        if (!isConnected) return

        // Construimos el objeto antes de enviarlo
        val messageMap = mutableMapOf<String, Any>()
        messageMap["action"] = action
        requestId?.let { messageMap["request_id"] = it }
        params?.let { messageMap.putAll(it) }

        // Gson se encarga de escapar comillas y caracteres raros
        val jsonString = gson.toJson(messageMap)

        Log.d(TAG, "Enviando: $jsonString")
        webSocket?.send(jsonString)
    }

    private fun retryConnection() {
        if (sessionManager?.isUserLoggedIn() == false) return
        currentUrl?.let { url ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isConnected) connect(url)
            }, 3000)
        }
    }
    // --- TEST DE CONEXIÓN ---
    fun testConnection(url: String, onResult: (Boolean, String) -> Unit) {
        // Usamos un cliente temporal con un timeout corto para no hacer esperar al usuario
        val testClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        testClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Si abre, la URL es válida y el servidor responde
                webSocket.close(1000, "Test exitoso")
                Handler(Looper.getMainLooper()).post {
                    onResult(true, "Conexión exitosa")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Si falla, devolvemos el error amigable
                Handler(Looper.getMainLooper()).post {
                    onResult(false, t.message ?: "Servidor no alcanzado")
                }
            }
        })
    }
    fun close() {
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        isConnected = false
    }
}