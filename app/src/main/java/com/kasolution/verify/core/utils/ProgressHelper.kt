package com.kasolution.verify.core.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kasolution.verify.databinding.DialogCustomProgressBinding

object ProgressHelper {

    private var progressDialog: AlertDialog? = null

    fun showProgress(context: Context, message: String? = null) {
        // Evitamos duplicados para no encimar diálogos
        if (progressDialog?.isShowing == true) return

        // 1. Inflar usando View Binding
        val binding = DialogCustomProgressBinding.inflate(LayoutInflater.from(context))

        // 2. Configurar mensaje si existe
        message?.let {
            binding.tvProgressMessage.text = it
            binding.tvProgressMessage.visibility = View.VISIBLE
        }

        // 3. Construir el diálogo
        progressDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        progressDialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.5f)
        }

        progressDialog?.show()

        progressDialog?.window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun hideProgress() {
        try {
            if (progressDialog?.isShowing == true) {
                progressDialog?.dismiss()
            }
        } catch (e: Exception) {
            // Manejo de excepción en caso de que la Activity ya no exista
        } finally {
            progressDialog = null
        }
    }
}