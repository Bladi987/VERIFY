package com.kasolution.verify.UI.Cash.fragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kasolution.verify.R
import com.kasolution.verify.UI.Cash.viewModel.CashViewModel
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.TicketManager
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.LayoutCashCloseSheetBinding // Asegúrate que este sea el nombre de tu XML
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class CashCloseSheet : BottomSheetDialogFragment() {
    private val tag = "CashCloseSheet"
    private var _binding: LayoutCashCloseSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by activityViewModels()
    private var reportPrinted = false

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                if (!reportPrinted) {
                    showExitConfirmation()
                    return@setOnKeyListener true
                }
            }
            false
        }

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                it.elevation = 0f
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutCashCloseSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomSheetBehavior()

        viewModel.closeReport.observe(viewLifecycleOwner) { data ->
            data?.let {
                Log.d(tag, "Reporte de cierre : $it")
                renderCashReport(it)
            }
        }
    }

    private fun renderCashReport(data: Map<String, Any>) {
        val business = data["business"] as? Map<String, Any> ?: emptyMap()
        val report = data["report"] as? Map<String, Any> ?: emptyMap()

        with(binding) {
            layoutReportItems.removeAllViews()

            // 1. Datos de Empresa
            tvHead.text =
                "${business["nombre_negocio"]}\nRUC: ${business["ruc"]}\n${business["direccion"]}"
            val idSesion = report["id_sesion"] ?: "No definido"
            tvSubHead.text = "REPORTE DE CIERRE DE CAJA\nID SESIÓN: $idSesion"

            // 2. Información del Cierre
            val cajero = report["empleado_nombre"] ?: "No definido"
            val fApertura = report["fecha_apertura"] ?: "--"
            val fCierre = report["fecha_cierre"] ?: "--"
            tvTicketInfo.text = "CAJERO: $cajero\nAPERTURA: $fApertura\nCIERRE: $fCierre"

            // 3. Desglose detallado del Movimiento de Efectivo
            addReportRow("MONTO APERTURA", report["monto_apertura"])
            addReportRow("(+) VENTAS EFECTIVO", report["ventas_efectivo"]) // Nuevo campo
            addReportRow("(+) OTROS INGRESOS", report["otros_ingresos"])
            addReportRow("(-) EGRESOS/COMPRAS", report["total_egresos"])

            // 4. Sección de Información Digital (No afecta el saldo físico esperado)
            layoutReportItems.addView(createSeparator())
            addReportRow("SALDO ESPERADO (EFEC)", report["monto_estimado"], true)
            addReportRow("SALDO REAL (CONTADO)", report["monto_real"], true)

            val diferencia = report["diferencia"]?.toString()?.toDoubleOrNull() ?: 0.0
            addDiferenciaStatus(diferencia)
            layoutReportItems.addView(createSeparator())
            addSectionHeader("DETALLE VENTAS DIGITALES")

            // Desglose detallado por método
            addReportRow("VENTAS YAPE", report["ventas_yape"])
            addReportRow("VENTAS PLIN", report["ventas_plin"])
            addReportRow("VENTAS TARJETA", report["ventas_tarjeta"])
            addReportRow("TRANSFERENCIAS", report["ventas_transferencia"])

            layoutReportItems.addView(createSeparator())
            addReportRow("TOTAL DIGITAL", report["ventas_digitales"], isBold = true)

            // 6. Resumen Final de Ventas (Total del Día)
            val totalDia = (report["ventas_efectivo"]?.toString()?.toDoubleOrNull() ?: 0.0) +
                    (report["ventas_digitales"]?.toString()?.toDoubleOrNull() ?: 0.0)

            addReportRow("VENTA TOTAL DEL DÍA", totalDia, isBold = true)

            tvNombreFirma.text = "Firma de: $cajero"
            tvNombreFirma.setOnClickListener { signaturePad.clear() }

            btnImprimirReporte.setOnClickListener {
                if (signaturePad.isEmpty) {
                    ToastHelper.clasicCustomToast(
                        binding.root,
                        "Por favor, firme el reporte",
                        false
                    )
                } else {
                    val fileName = "Cierre_Caja_${report["id_sesion"]}"
                    TicketManager.generateTicketPdf(
                        requireContext(),
                        binding.scrollTicketCash,
                        fileName
                    )?.let { file ->
                        TicketManager.shareTicket(requireContext(), file)
                        reportPrinted = true
                        (dialog as? BottomSheetDialog)?.behavior?.isHideable = true
                    }
                    dismiss()
                }
            }
        }
    }

    private fun addSectionHeader(title: String) {
        binding.layoutReportItems.addView(TextView(context).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 5)
        })
    }

    private fun addReportRow(label: String, value: Any?, isBold: Boolean = false) {
        val monto = value?.toString()?.toDoubleOrNull() ?: 0.0
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            weightSum = 10f
            setPadding(0, 8, 0, 8)
        }

        row.addView(TextView(context).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, -2, 7f)
            setTextColor(Color.BLACK)
            if (isBold) setTypeface(null, Typeface.BOLD)
        })

        row.addView(TextView(context).apply {
            text = String.format(Locale.US, "S/ %.2f", monto)
            layoutParams = LinearLayout.LayoutParams(0, -2, 3f)
            textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            setTextColor(Color.BLACK)
            if (isBold) setTypeface(null, Typeface.BOLD)
        })

        binding.layoutReportItems.addView(row)
    }

    private fun addDiferenciaStatus(dif: Double) {
        val color = if (dif < -0.01) "#F44336" else if (dif > 0.01) "#4CAF50" else "#2196F3"
        val texto = when {
            dif < -0.01 -> "FALTANTE: S/ ${String.format(Locale.US, "%.2f", dif)}"
            dif > 0.01 -> "SOBRANTE: S/ ${String.format(Locale.US, "%.2f", dif)}"
            else -> "CAJA CUADRADA"
        }

        binding.layoutReportItems.addView(TextView(context).apply {
            text = texto
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(color))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 20, 0, 10)
        })
    }

    private fun createSeparator(): View = View(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(-1, (2 * resources.displayMetrics.density).toInt()).apply {
                val margin = (8 * resources.displayMetrics.density).toInt()
                setMargins(0, margin, 0, margin)
            }
        setBackgroundResource(R.drawable.line_dashed)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private fun setupBottomSheetBehavior() {
        val behavior = (dialog as? BottomSheetDialog)?.behavior
        behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = true // Debe ser true para detectar el gesto de salida

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Si por algún motivo llega a ocultarse sin permiso, lo regresamos
                    if (newState == BottomSheetBehavior.STATE_HIDDEN && !reportPrinted) {
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // slideOffset va de 0 (expandido) a -1 (oculto)
                    // Si el usuario ya deslizó más de la mitad, podemos preparar el bloqueo
                    if (slideOffset < -0.5 && !reportPrinted) {

                        // Forzamos a que regrese arriba
                        state = BottomSheetBehavior.STATE_EXPANDED

                        // Mostramos el diálogo
                        showExitConfirmation()
                    }
                }
            })
        }
    }

    private fun showExitConfirmation() {


        val behavior = (dialog as? BottomSheetDialog)?.behavior
        DialogHelper.showConfirmation(
            requireContext(),
            "Reporte no impreso",
            "¿Deseas cerrar el reporte sin imprimir? Esta acción no se puede deshacer.",
            "Cerrar",
            "Imprimir ahora", onConfirm = {
                reportPrinted = true
                behavior?.isHideable = true // Habilitamos para que dismiss() funcione
                dismiss()
            },
            onCancel = {
                behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetOperationStatus()
        _binding = null
    }
}