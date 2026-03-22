package com.kasolution.verify.UI.Sales.fragment

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
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kasolution.verify.R
import com.kasolution.verify.UI.Sales.History.HistoryActivity
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModel
import com.kasolution.verify.core.utils.NumberToLetterConverter
import com.kasolution.verify.databinding.LayoutTicketSheetBinding
import java.io.File
import java.io.FileOutputStream

class SaleDetailSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutTicketSheetBinding? = null
    private val binding get() = _binding!!
    private var isFromHistory: Boolean = false

    private val viewModel: SalesViewModel by activityViewModels()

    // 1. Vinculamos el tema personalizado
    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // 2. Limpieza de fondo y sombras del sistema para el efecto "Floating"
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
        _binding = LayoutTicketSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFromHistory = arguments?.getBoolean(ARG_IS_FROM_HISTORY) ?: false
        setupBottomSheetBehavior()

        binding.btnAnularVenta.visibility = if (isFromHistory) View.VISIBLE else View.GONE

        viewModel.invoiceFullData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                renderFullInvoice(data)
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { action ->
            if (action == "SALE_DELETE") {
                dismiss()
            }
        }
    }

    private fun renderFullInvoice(data: Map<String, Any>) {
        val business = data["business"] as? Map<String, Any> ?: emptyMap()
        val header = data["header"] as? Map<String, Any> ?: emptyMap()
        val items = data["items"] as? List<Map<String, Any>> ?: emptyList()
        val estadoVenta = header["estado"]?.toString() ?: "ACTIVO"
        val esAnulada = estadoVenta.equals("ANULADO", ignoreCase = true) || estadoVenta == "0"

        with(binding) {
            layoutProductItems.removeAllViews()

            if (esAnulada) {
                btnAnularVenta.visibility = View.GONE
                btnImprimirDuplicado.visibility = View.GONE
                ivSelloAnulado.isVisible = true
            } else {
                btnAnularVenta.isVisible = isFromHistory
                btnImprimirDuplicado.visibility = View.VISIBLE
                ivSelloAnulado.isVisible = false
            }

            val idVenta = header["id_venta"]?.toString() ?: ""

            btnAnularVenta.setOnClickListener {
                if (idVenta.isNotEmpty()) {
                    (requireActivity() as? HistoryActivity)?.confirmAnnul(idVenta.toDouble().toInt())
                } else {
                    Toast.makeText(context, "ID de venta no encontrado", Toast.LENGTH_SHORT).show()
                }
            }

            btnImprimirDuplicado.setOnClickListener {
                val nroComprobante = tvTicketComprobanteNro.text.toString().replace("\n", "_")
                val pdfFile = generarPdfTicket(scrollTicket, "Ticket_$nroComprobante")
                pdfFile?.let { compartirArchivo(it) }
                dismiss()
            }

            // Datos de la Empresa
            tvTicketId.text = "${business["nombre_negocio"]}\nRUC: ${business["ruc"]}\n${business["direccion"]}"

            // Tipo de Comprobante
            val tipo = header["tipo_nombre"]?.toString() ?: "COMPROBANTE"
            val serie = header["serie"]?.toString() ?: ""
            val corr = header["correlativo"]?.toString()?.toDouble()?.toInt()?.toString()?.padStart(8, '0') ?: ""
            tvTicketComprobanteNro.text = "$tipo ELECTRÓNICA\n$serie-$corr"

            // Datos de la Venta
            val cliente = header["cliente_nombre"]?.toString() ?: "PÚBLICO GENERAL"
            val doc = header["cliente_documento"]?.toString() ?: "----------"
            val vendedor = header["empleado_nombre"]?.toString() ?: "Cajero Principal"
            tvTicketFecha.text = "FECHA: ${header["fecha"]}\nMÉTODO: ${header["metodo_pago"]}\nCLIENTE: $cliente\nDOC: $doc\nLE ATENDIÓ: $vendedor"

            renderProducts(items)

            // Totales
            val total = header["total"].toString().toDoubleOrNull() ?: 0.0
            val tasa = business["impuesto"]?.toString()?.toDoubleOrNull() ?: 18.0
            val subtotal = total / (1 + (tasa / 100))
            val igv = total - subtotal

            layoutProductItems.addView(createSeparator())

            val layoutTotales = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            }

            layoutTotales.addView(createTotalRow("OP. GRAVADA:", subtotal))
            layoutTotales.addView(createTotalRow("IGV (${tasa.toInt()}%):", igv))
            layoutTotales.addView(createTotalRow("TOTAL A PAGAR:", total, true))

            layoutProductItems.addView(layoutTotales)
            layoutProductItems.addView(createSeparator())

            // Monto en Letras
            layoutProductItems.addView(createTicketTextView("SON: ${convertirMontoALetras(total)}", 0).apply {
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(-1, -2)
                setPadding(0, 5, 0, 10)
            })

            // Leyenda SUNAT
            layoutProductItems.addView(TextView(context).apply {
                text = "Representación impresa de la ${tipo.uppercase()} ELECTRÓNICA.\nConsulte su comprobante en: www.sunat.gob.pe"
                textSize = 9f
                setTextColor(Color.BLACK)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 10, 0, 10)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            })

            // QR
            if (idVenta.isNotEmpty()) {
                ivTicketQR.setImageBitmap(generateQRCode(idVenta))
                ivTicketQR.visibility = View.VISIBLE
                ivTicketQR.layoutParams = (ivTicketQR.layoutParams as LinearLayout.LayoutParams).apply {
                    height = (140 * resources.displayMetrics.density).toInt()
                    width = (140 * resources.displayMetrics.density).toInt()
                    weight = 0f
                    topMargin = 10
                }
            }

            // Footer
            val telefono = business["telefono"]?.toString() ?: ""
            val celular = business["celular"]?.toString() ?: ""
            val contactoStr = if (celular.isNotEmpty()) "WhatsApp: $celular" else "Tel: $telefono"

            layoutProductItems.addView(TextView(context).apply {
                text = "¡GRACIAS POR SU COMPRA!\n$contactoStr"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 15, 0, 15)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            })
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.Animation_iOS_Sheet)
    }

    private fun renderProducts(productos: List<Map<String, Any>>) {
        productos.forEach { item ->
            val rowContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2)
                weightSum = 10f
                setPadding(0, 10, 0, 10)
            }

            val cant = item["cantidad"].toString().toDouble().toInt()
            val um = item["unidad_medida"]?.toString() ?: "UND"
            val nombre = item["producto_nombre"]?.toString() ?: "Producto"
            val codigo = item["producto_codigo"]?.toString() ?: ""
            val textoAMostrar = if (codigo.isNotEmpty()) "$nombre\n[$codigo]" else nombre
            val precio = item["precio_unitario"].toString().toDouble()

            rowContainer.addView(createTicketTextView("${cant}x", 0, 1.2f))
            rowContainer.addView(createTicketTextView(um, 0, 1.5f))
            rowContainer.addView(createTicketTextView(textoAMostrar, 0, 4.8f).apply {
                isSingleLine = false
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            })
            rowContainer.addView(createTicketTextView(
                String.format(java.util.Locale.US, "%.2f", cant * precio),
                0, 2.5f, View.TEXT_ALIGNMENT_TEXT_END
            ).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) })

            binding.layoutProductItems.addView(rowContainer)
        }
    }

    private fun createSeparator(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(-1, (2 * resources.displayMetrics.density).toInt()).apply {
            val margin = (8 * resources.displayMetrics.density).toInt()
            setMargins(0, margin, 0, margin)
        }
        setBackgroundResource(R.drawable.line_dashed)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private fun createTotalRow(label: String, value: Double, isBold: Boolean = false): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            weightSum = 10f
        }
        row.addView(createTicketTextView(label, 0, 7f, View.TEXT_ALIGNMENT_TEXT_END).apply {
            if (isBold) typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        row.addView(createTicketTextView(
            String.format(java.util.Locale.US, " S/ %.2f", value), 0, 3f, View.TEXT_ALIGNMENT_TEXT_END
        ).apply {
            if (isBold) typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        return row
    }

    private fun setupBottomSheetBehavior() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = true // 3. Habilitamos el cierre táctil
        }
    }

    private fun createTicketTextView(text: String, widthDp: Int = 0, weight: Float = 0f, textAlign: Int = View.TEXT_ALIGNMENT_TEXT_START): TextView {
        return TextView(context).apply {
            this.text = text
            this.textSize = 12f
            this.textAlignment = textAlign
            this.setTextColor(Color.BLACK)
            val layoutWidth = if (weight > 0f) 0 else (widthDp * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(layoutWidth, -2, weight)
        }
    }

    private fun convertirMontoALetras(monto: Double): String {
        val entero = monto.toLong()
        val centavos = ((monto - entero) * 100).toInt()
        return "${NumberToLetterConverter.convert(entero)} CON ${String.format("%02d", centavos)}/100 SOLES"
    }

    private fun generateQRCode(text: String): android.graphics.Bitmap? {
        val width = 400
        val height = 400
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, height)
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }

    private fun generarPdfTicket(scrollView: NestedScrollView, nombreArchivo: String): File? {
        val childView = scrollView.getChildAt(0)
        val margin = 30
        val pdfWidth = childView.width + (margin * 2)
        val pdfHeight = childView.height + (margin * 2)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.translate(margin.toFloat(), margin.toFloat())
        childView.draw(page.canvas)
        document.finishPage(page)
        return try {
            val file = File(requireContext().cacheDir, "$nombreArchivo.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun compartirArchivo(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Enviar Ticket por:"))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val ARG_IS_FROM_HISTORY = "is_from_history"
        fun newInstance(isFromHistory: Boolean): SaleDetailSheet {
            return SaleDetailSheet().apply {
                arguments = Bundle().apply { putBoolean(ARG_IS_FROM_HISTORY, isFromHistory) }
            }
        }
    }
}