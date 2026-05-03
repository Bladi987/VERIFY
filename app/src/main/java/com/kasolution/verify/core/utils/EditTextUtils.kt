package com.kasolution.verify.core.utils

import android.text.InputFilter
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * Configura el EditText para manejar entrada de moneda (2 decimales y formato al perder foco).
 */
fun TextInputEditText.setupCurrencyFormatting() {
    val decimalFilter = InputFilter { source, _, _, dest, dstart, dend ->
        val builder = StringBuilder(dest)
        builder.replace(dstart, dend, source.toString())

        // RegEx: Solo números y máximo un punto con dos decimales
        val regex = Regex("^\\d*(\\.\\d{0,2})?$")
        if (!builder.toString().matches(regex)) {
            if (source.isEmpty()) dest.subSequence(dstart, dend) else ""
        } else null
    }

    this.filters = arrayOf(decimalFilter)

    this.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            val text = this.text.toString()
            if (text.isNotEmpty()) {
                val parsed = text.toDoubleOrNull()
                if (parsed != null) {
                    // Usar Locale.US asegura que el punto decimal sea siempre "."
                    // y no una coma según el idioma del teléfono
                    this.setText(String.format(Locale.US, "%.2f", parsed))
                } else {
                    this.text = null
                }
            }
        } else {
            this.selectAll()
        }
    }
}