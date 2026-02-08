package com.kasolution.verify.UI.Suppliers

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kasolution.verify.R
import com.kasolution.verify.databinding.ActivitySuppliersBinding

class SuppliersActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuppliersBinding

    private var isLoadingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivitySuppliersBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //binding.btnSaveClient.setText(if (isEdit) "ACTUALIZAR" else "GUARDAR")

        // 1. Configurar el texto inicial
        binding.btnSaveClient.setText("ENVIAR DATOS")

        // 2. Configurar el click del componente personalizado
        binding.btnSaveClient.setOnClickListener {
            isLoadingState = true
            binding.btnSaveClient.setLoading(true)
        }

        // 3. Botón de auxilio para detener la carga y probar que vuelve a la normalidad
        binding.btnToggleLoading.setOnClickListener {
            isLoadingState = !isLoadingState
            binding.btnSaveClient.setLoading(isLoadingState)
        }


    }
}