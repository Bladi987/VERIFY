package com.kasolution.verify.core.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
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
import com.kasolution.verify.databinding.DialogChangePriceBinding
import com.kasolution.verify.databinding.DialogChangeQuantityBinding
import com.kasolution.verify.databinding.DialogConfigServerBinding

object DialogHelper {
    fun createBaseDialog(context: Context, view: View): AlertDialog {
        val dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setWindowAnimations(R.style.AnimationiOSDialog)
        }
        dialog.setCancelable(false)
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        return dialog
    }

    private fun View.applyIOSClick(onAnimationEnd: () -> Unit) {
        this.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(80)
            .withEndAction {
                this.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction { onAnimationEnd() }
                    .start()
            }
            .start()
    }

    private fun View.startShake() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.shake)
        this.startAnimation(animation)
        this.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
    }

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
            it.applyIOSClick {
                onConfirm?.invoke()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // 2. CONFIRMACIÓN (Dos Botones)
    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        textPositiveButton:String = "CONFIRMAR",
        textNegativeButton:String = "CANCELAR",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val binding = CustomDialogConfirmationBinding.inflate(LayoutInflater.from(context))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message
        binding.btnConfirm.text = textPositiveButton
        binding.btnCancel.text = textNegativeButton

        val dialog = createBaseDialog(context, binding.root)
        binding.btnConfirm.setOnClickListener {
            it.applyIOSClick {
                onConfirm()
                dialog.dismiss()
            }
        }
        binding.btnCancel.setOnClickListener {
            it.applyIOSClick {
                onCancel?.invoke()
                dialog.dismiss()
            }
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
            it.applyIOSClick {
                val text = binding.etInput.text.toString()
                if (text.isNotBlank()) {
                    onResult(text)
                    dialog.dismiss()
                } else {
                    binding.inputLayout.error = "Campo requerido"
                    binding.root.startShake()
                }
            }
        }
        dialog.show()
    }

    // FUNCIÓN PRIVADA PARA EVITAR REPETIR CONFIGURACIÓN DE VENTANA

    fun showQuantityDialog(
        context: Context,
        productName: String,
        currentQuantity: Int,
        @ColorRes colorRes: Int = R.color.blue_corporative_primary,
        onResult: (Int) -> Unit
    ) {
        val binding = DialogChangeQuantityBinding.inflate(LayoutInflater.from(context))
        val color = ContextCompat.getColor(context, colorRes)

        // Configuración inicial
        binding.tvDialogTitle.text = productName
        binding.etQuantity.setText(currentQuantity.toString())
        binding.etQuantity.setSelectAllOnFocus(true)
        binding.ivDialogIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(color)
        binding.tilQuantity.boxStrokeColor = color
        binding.tilQuantity.hintTextColor = ColorStateList.valueOf(color)

        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            it.applyIOSClick {
                val nuevaCant = binding.etQuantity.text.toString().toIntOrNull()
                if (nuevaCant != null && nuevaCant > 0) {
                    onResult(nuevaCant)
                    dialog.dismiss()
                } else {
                    binding.tilQuantity.error = "Cantidad inválida"
                    binding.root.startShake()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            it.applyIOSClick {
                dialog.dismiss()
            }
        }

        dialog.show()
        // Forzar teclado
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.etQuantity.requestFocus()
    }

    // 6. DIÁLOGO DE PRECIO (Compras)
    fun showPriceDialog(
        context: Context,
        productName: String,
        currentPrice: Double,
        @ColorRes colorRes: Int = R.color.blue_corporative_primary,
        onResult: (Double) -> Unit
    ) {
        val binding = DialogChangePriceBinding.inflate(LayoutInflater.from(context))
        val color = ContextCompat.getColor(context, colorRes)

        binding.tvDialogTitle.text = productName
        binding.etPrice.setText(String.format(java.util.Locale.US, "%.2f", currentPrice))
        binding.etPrice.setSelectAllOnFocus(true)

        binding.ivDialogIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(color)
        binding.tilPrice.boxStrokeColor = color
        binding.tilPrice.hintTextColor = ColorStateList.valueOf(color)
        binding.tilPrice.setPrefixTextColor(ColorStateList.valueOf(color))

        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            it.applyIOSClick {
                val nuevoPrecio = binding.etPrice.text.toString().toDoubleOrNull()
                if (nuevoPrecio != null && nuevoPrecio >= 0) {
                    onResult(nuevoPrecio)
                    dialog.dismiss()
                } else {
                    binding.tilPrice.error = "Precio inválido"
                    binding.root.startShake()
                }
            }
        }

        binding.btnCancel.setOnClickListener { it.applyIOSClick { dialog.dismiss() } }

        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.etPrice.requestFocus()
    }

    fun showConfigDialog(context: Context, onConfigSaved: () -> Unit) {
        val settings = SettingsManager(context)
        val binding = DialogConfigServerBinding.inflate(LayoutInflater.from(context))

        binding.etIp.setText(settings.getIp())
        binding.etPort.setText(settings.getPort())
        val dialog = createBaseDialog(context, binding.root)

        binding.btnConfirm.setOnClickListener {
            it.applyIOSClick {
                val ip = binding.etIp.text.toString().trim()
                val port = binding.etPort.text.toString().trim()

                // 1. Validaciones
                var hasError = false
                if (!isValidIpOrHost(ip)) {
                    binding.tilIp.error = "IP o Host Inválido"
                    binding.root.startShake()
                    hasError = true
                }
                if (port.isEmpty()) {
                    binding.tilPort.error = "Puerto requerido"
                    binding.root.startShake()
                    hasError = true
                }

                // Si hay error, no continuamos.
                // Al ser la última línea de la lambda, simplemente no hace nada más.
                if (!hasError) {
                    // 2. Estado visual de "Probando conexión"
                    binding.btnConfirm.isEnabled = false
                    binding.btnConfirm.text = "Probando..."
                    val testUrl = "ws://$ip:$port"

                    // 3. Prueba de conexión
                    SocketManager.getInstance().testConnection(testUrl) { success, message ->
                        (context as? Activity)?.runOnUiThread {
                            if (!success) {
                                binding.btnConfirm.isEnabled = true
                                binding.btnConfirm.text = "Probar y Guardar"
                                ToastHelper.showCustomToast(binding.root, "Fallo de conexión", false)
                                binding.tilIp.error = "IP no valida"
                                binding.root.startShake()
                            } else {
                                dialog.dismiss()
                                settings.saveConfig(ip, port)
                                onConfigSaved()
                            }
                        }
                    }
                }
            }
        }
        binding.btnCancel.setOnClickListener { it.applyIOSClick { dialog.dismiss() } }
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