package com.kasolution.verify.UI.Access

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.kasolution.verify.R
import com.kasolution.verify.UI.Access.viewModel.LoginViewModel
import com.kasolution.verify.UI.Dashboard.Dashboard
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = AppProvider.provideSessionManager(this)
        if (sessionManager.isUserLoggedIn()) {
            irADashboard()
            return // Finalizamos el onCreate aquí
        }
        AppProvider.connectSocket(this)
        // 2. CONFIGURACIÓN DE VISTA (Si no hay sesión)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar ViewModel
        val factory = AppProvider.provideLoginViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setupObservers()

        binding.btnLogin.setOnClickListener {
            setupLogin()
        }
        binding.btnSettings.setOnClickListener {
            DialogHelper.showConfigDialog(this) {
                // Al cambiar la IP, reconectamos el socket
                AppProvider.connectSocket(this)
                ToastHelper.showCustomToast(binding.root, "Servidor actualizado", true)
                //showMessage("Servidor actualizado")
                //Toast.makeText(this, "Servidor actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { response ->
            if (response == null) return@observe
            // Siempre restauramos el estado de la UI al recibir respuesta
            binding.btnLogin.isEnabled = true
            Log.e("LoginActivity", "action: ${response.action}")
            Log.e("LoginActivity", "estatus: ${response.status}")
            if (response.status == "success") {
                irADashboard()
                viewModel.resetState()
            } else if (response.action=="CONECTION_ERROR") {
                DialogHelper.showNotification(
                    this,
                    "Aviso",
                    response.message.toString(), R.drawable.ic_server_disconnect,
                    android.R.color.holo_red_light
                )
            }else{
                DialogHelper.showNotification(
                    this,
                    "Aviso",
                    response.message.toString(), R.drawable.ic_error,
                    android.R.color.holo_red_light
                )
            }
            binding.btnLogin.setLoading(false)
            binding.btnLogin.isEnabled = true
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.btnLogin.setLoading(loading)
        }
    }

    private fun setupLogin() {
        val usuario = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()


        if (usuario.isEmpty()){
            binding.tilUsername.error = "Por favor, ingresa tu nombre de usuario"
            return
        }else if (password.isEmpty()){
            binding.tilPassword.error = "Por favor, ingresa tu contraseña"
            return
        }

//        if (usuario.isEmpty() || password.isEmpty()) {
//            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
//            return
//        }
        binding.btnLogin.isEnabled = false

        viewModel.intentarLogin(usuario, password)
    }

    private fun irADashboard() {
        val intent = Intent(this, Dashboard::class.java)
        // Limpiamos el stack para que no pueda volver al login con el botón 'atrás'
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String, isError: Boolean = false) {
        val duration = if (isError) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
        val colorRes =
            if (isError) android.R.color.holo_red_light else android.R.color.holo_green_light

        Snackbar.make(binding.root, message, duration).apply {
            setBackgroundTint(ContextCompat.getColor(context, colorRes))
            if (isError) {
                setAction("CERRAR") { dismiss() }
                setActionTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
            show()
        }
    }

}