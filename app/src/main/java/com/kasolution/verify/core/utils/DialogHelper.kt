package com.kasolution.verify.core.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kasolution.verify.R
import com.kasolution.verify.data.local.SettingsManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.databinding.CustomDialogConfirmationBinding
import com.kasolution.verify.databinding.CustomDialogInputBinding
import com.kasolution.verify.databinding.CustomDialogNotificationBinding
import com.kasolution.verify.databinding.DialogConfigServerBinding

object DialogHelper {

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        @DrawableRes icon: Int = R.drawable.ic_info, // Icono por defecto
        @ColorRes colorRes: Int = android.R.color.holo_blue_dark, // Color por defecto
        onConfirm: (() -> Unit)? = null
    ) {
        val binding = CustomDialogNotificationBinding.inflate(LayoutInflater.from(context))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        // Configuramos icono y su color
        binding.ivDialogIcon.setImageResource(icon)
        binding.ivDialogIcon.setColorFilter(
            ContextCompat.getColor(context, colorRes),
            PorterDuff.Mode.SRC_IN
        )

        // También podemos pintar el botón del mismo color para que combine
        binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, colorRes)
        )

        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            onConfirm?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }

    // 2. CONFIRMACIÓN (Dos Botones)
    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val binding = CustomDialogConfirmationBinding.inflate(LayoutInflater.from(context))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        val dialog = createBaseDialog(context, binding.root)
        binding.btnConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }
        binding.btnCancel.setOnClickListener {
            onCancel?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }

    // 3. SELECCIÓN (Lista Simple)
    fun showListSelection(
        context: Context,
        title: String,
        items: Array<String>,
        onSelected: (Int) -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(context) // Usamos Material para listas más fácil
        builder.setTitle(title)
        builder.setItems(items) { _, which ->
            onSelected(which)
        }
        builder.show()
    }

    // 4. INPUT / FORMULARIO RÁPIDO
    fun showInput(
        context: Context,
        title: String,
        hint: String,
        onResult: (String) -> Unit
    ) {
        val binding = CustomDialogInputBinding.inflate(LayoutInflater.from(context))
        binding.tvDialogTitle.text = title
        binding.inputLayout.hint = hint

        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotBlank()) {
                onResult(text)
                dialog.dismiss()
            } else {
                binding.inputLayout.error = "Campo requerido"
            }
        }
        dialog.show()
    }

    // FUNCIÓN PRIVADA PARA EVITAR REPETIR CONFIGURACIÓN DE VENTANA
    private fun createBaseDialog(context: Context, view: View): AlertDialog {
        val dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
        return dialog
    }


    fun showConfigDialog(context: Context, onConfigSaved: () -> Unit) {
        val settings = SettingsManager(context)
        val binding = DialogConfigServerBinding.inflate(LayoutInflater.from(context))

        // Precarga de datos
        binding.etIp.setText(settings.getIp())
        binding.etPort.setText(settings.getPort())
        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            val ip = binding.etIp.text.toString().trim()
            val port = binding.etPort.text.toString().trim()

            // 1. Validaciones de formato previas
            if (!isValidIpOrHost(ip)) {
                binding.etIp.error = "IP o Host Inválido"
                return@setOnClickListener
            }
            if (port.isEmpty()) {
                binding.etPort.error = "Puerto requerido"
                return@setOnClickListener
            }

            // 2. Estado visual de "Probando conexión"
            binding.btnConfirm.isEnabled = false
            binding.btnConfirm.text = "Probando..."
            val testUrl = "ws://$ip:$port"

            // 3. Prueba de conexión real a través del SocketManager
            // Usamos una instancia temporal o un método de prueba en SocketManager
            SocketManager.getInstance().testConnection(testUrl) { success, message ->
                // Regresamos al hilo principal para actualizar la UI
                (context as? Activity)?.runOnUiThread {
                    if (!success) {
                        // ERROR: Notificamos y rehabilitamos el botón
                        binding.btnConfirm.isEnabled = true
                        binding.btnConfirm.text = "Probar y Guardar"
//                        Toast.makeText(context, "Fallo de conexión: $message", Toast.LENGTH_LONG).show()
                        ToastHelper.showCustomToast(binding.root, "Fallo de conexión", false)
                        binding.etIp.error = "IP no valida"
                    } else {
                        dialog.dismiss()
                        settings.saveConfig(ip, port)
                        onConfigSaved()
                    }
                }
            }
        }
        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun isValidIpOrHost(target: String): Boolean {
        val ipPattern =
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        val hostPattern =
            "^(?=.{1,253}$)(?:(?!-)[a-zA-Z0-9-]{1,63}(?<!-)\\.)+[a-zA-Z]{2,63}$|localhost"
        return target.matches(ipPattern.toRegex()) || target.matches(hostPattern.toRegex())
    }
}