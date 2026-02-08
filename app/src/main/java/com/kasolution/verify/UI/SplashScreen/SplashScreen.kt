package com.kasolution.verify.UI.SplashScreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kasolution.verify.UI.Access.LoginActivity
import com.kasolution.verify.UI.Dashboard.Dashboard
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.data.local.SettingsManager
import com.kasolution.verify.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivitySplashScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val settings = SettingsManager(this)

        if (!settings.isConfigured()) {
            DialogHelper.showConfigDialog(this) {
                continuarFlujoApp() // Acción después de guardar
            }
        } else {
            continuarFlujoApp()
        }
    }
    private fun continuarFlujoApp() {
        // 1. Conectar Socket con la IP guardada
        AppProvider.connectSocket(this)

        // 2. Verificar Sesión (Auto-Login)
        val session = AppProvider.provideSessionManager(this)
        if (session.isUserLoggedIn()) {
            startActivity(Intent(this, Dashboard::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

}