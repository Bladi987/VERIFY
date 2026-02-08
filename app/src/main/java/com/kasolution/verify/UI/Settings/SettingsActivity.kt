package com.kasolution.verify.UI.Settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnUpdateUrl.setOnClickListener {
            DialogHelper.showConfigDialog(this) {
                // Al cambiar la IP, reconectamos el socket
                AppProvider.connectSocket(this)
                Toast.makeText(this, "Servidor actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}