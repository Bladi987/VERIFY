package com.kasolution.verify

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kasolution.verify.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var WEBSOCKET_URL = ""
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)




        binding.btnConectar.setOnClickListener {
            // 1. Conexión WebSocket al inicio de la aplicación
            //obtenemos ip y puerto
            WEBSOCKET_URL = "ws://${binding.etIP.text}:8080"
            Toast.makeText(this,"conectando a la ip $WEBSOCKET_URL",Toast.LENGTH_SHORT).show()
            connectToWebSocket()
        }

        binding.btnEnviar.setOnClickListener {
            val messageToSend = binding.etMessage.text.toString()
            if (messageToSend.isNotEmpty()) {
                sendMessage(messageToSend)
            } else {
                Toast.makeText(
                    this,
                    "Por favor, escribe un mensaje en el campo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun connectToWebSocket() {
        val request = Request.Builder().url(WEBSOCKET_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            // CONEXIÓN ABIERTA
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "WebSocket CONECTADO.", Toast.LENGTH_LONG)
                        .show()
                }
            }

            // RECEPCIÓN DE MENSAJES (Muestra cualquier respuesta del servidor)
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    binding.tvResult.text = "Servidor responde: $text"
                }
            }

            // ERRORES
            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: okhttp3.Response?
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "WebSocket FALLO: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvResult.text = "Fallo de conexión: ${t.message}"
                }
            }

            // CIERRE DE CONEXIÓN
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "WebSocket CERRADO.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        })
    }

    private fun sendMessage(content: String) {
        // Enviar el mensaje, manteniendo la estructura JSON con la clave "barcode"
        // para que sea compatible con tu lógica de inserción de MySQL.
        val message = "{\"barcode\": \"$content\"}"

        if (webSocket?.send(message) == true) {
            Toast.makeText(this, "Enviando: $content", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "ERROR: WebSocket no está conectado.", Toast.LENGTH_SHORT).show()
            connectToWebSocket() // Intenta reconectar
        }
    }

    // ----------------------------------------------------------------------

    override fun onDestroy() {
        super.onDestroy()
        // Cierra el WebSocket limpiamente al cerrar la actividad
        webSocket?.close(1000, "App cerrada")
        client.dispatcher.executorService.shutdown()
    }
}