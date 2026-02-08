package com.kasolution.verify.core.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kasolution.verify.R
import com.kasolution.verify.databinding.LayoutCustomToastBinding

object ToastHelper {

    fun showCustomToast(view: View, message: String, isSuccess: Boolean) {
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
        val context = view.context

        // 1. Inflar usando el Binding generado para layout_custom_toast.xml
        val binding = LayoutCustomToastBinding.inflate(LayoutInflater.from(context))

        // 2. Configurar los datos directamente a través del binding
        binding.toastText.text = message

        // Definir recursos según el estado
        val colorRes = if (isSuccess) R.color.semantic_success_green else R.color.semantic_error_red
        val iconRes = if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_error

        // Aplicar estilos a la Card y al Icono usando el binding
        binding.toastCard.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        binding.toastIcon.setImageResource(iconRes)

        // 3. Configurar el contenedor del Snackbar
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout

        snackbarLayout.apply {
            setBackgroundColor(Color.TRANSPARENT) // Quitamos el fondo por defecto
            setPadding(0, 0, 0, 0)
            elevation = 0f // La elevación la maneja tu MaterialCardView

            removeAllViews()
            addView(binding.root) // Añadimos el root del layout inflado con binding
        }

        snackbar.show()
    }
}