package com.kasolution.verify.core.utils

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
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
    fun clasicCustomToast(vista: View, mensaje: String?, isSuccess: Boolean){
        val context = vista.context

        val binding = LayoutCustomToastBinding.inflate(LayoutInflater.from(context))
        val anim1 = AnimationUtils.loadAnimation(context, R.anim.anim_toast_enter)
        val colorRes = if (isSuccess) R.color.semantic_success_green else R.color.semantic_error_red
        val iconRes = if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_error
        binding.toastCard.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        binding.toastIcon.setImageResource(iconRes)
        binding.toastText.text=mensaje
        binding.toastCard.startAnimation(anim1)
        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
            view = binding.root
            show()
        }
    }
}