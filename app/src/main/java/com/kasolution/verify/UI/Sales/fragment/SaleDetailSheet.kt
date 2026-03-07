package com.kasolution.verify.UI.Sales.fragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.Gravity
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
import com.kasolution.verify.UI.Sales.History.HistoryActivity
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModel
import com.kasolution.verify.core.utils.NumberToLetterConverter
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.LayoutTicketSheetBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class SaleDetailSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutTicketSheetBinding? = null
    private val binding get() = _binding!!
    private var isFromHistory: Boolean = false

    // Usamos activityViewModels para compartir la misma instancia que la Activity
    private val viewModel: SalesViewModel by activityViewModels()

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
                Log.d("SaleDetailSheet", "Datos completos recibidos: $data")
                renderFullInvoice(data)
            }
        }
        viewModel.operationSuccess.observe(viewLifecycleOwner){action ->
            if (action == "SALE_DELETE") {
                dismiss()
//                ToastHelper.showCustomToast(binding.root, "Venta anulada con éxito",true)

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
            // 1. Limpieza total del contenedor dinámico
            layoutProductItems.removeAllViews()
            if (esAnulada) {
                // 5. Ocultamos los botones de acción
                binding.btnAnularVenta.visibility = View.GONE
                binding.btnImprimirDuplicado.visibility = View.GONE
                binding.ivSelloAnulado.isVisible=true
            } else {
                // Si no es anulada, nos aseguramos de que los botones existan
                btnAnularVenta.isVisible=isFromHistory
                binding.btnImprimirDuplicado.visibility = View.VISIBLE
                binding.ivSelloAnulado.isVisible=false
            }


            val idVenta = header["id_venta"]?.toString() ?: ""

            // 2. Reasignamos la funcionalidad al botón ANULAR
            btnAnularVenta.setOnClickListener {
                if (idVenta.isNotEmpty()) {
                    // Aquí llamamos a tu función de confirmación de anulación
                    (requireActivity() as? HistoryActivity)?.confirmAnnul(idVenta.toDouble().toInt())
                } else {
                    Toast.makeText(context, "No se encontró el ID de la venta", Toast.LENGTH_SHORT).show()
                }
            }


            // El botón Imprimir ya lo configuramos para el PDF
            btnImprimirDuplicado.setOnClickListener {
                val nroComprobante = tvTicketComprobanteNro.text.toString().replace("\n", "_")
                val pdfFile = generarPdfTicket(scrollTicket, "Ticket_$nroComprobante")
                pdfFile?.let { compartirArchivo(it) }
                dismiss()
            }

            // 2. Cabecera - Datos de la Empresa
            tvTicketId.text =
                "${business["nombre_negocio"]}\nRUC: ${business["ruc"]}\n${business["direccion"]}"

            // 3. Cabecera - Tipo y Número de Comprobante
            val tipo = header["tipo_nombre"]?.toString() ?: "COMPROBANTE"
            val serie = header["serie"]?.toString() ?: ""
            val corr =
                header["correlativo"]?.toString()?.toDouble()?.toInt()?.toString()?.padStart(8, '0')
                    ?: ""
            tvTicketComprobanteNro.text = "$tipo ELECTRÓNICA\n$serie-$corr"

            // 4. Datos de la Venta (Fecha, Cliente, Método)
            val cliente = header["cliente_nombre"]?.toString() ?: "PÚBLICO GENERAL"
            val doc = header["cliente_documento"]?.toString() ?: "----------"
            val vendedor = header["empleado_nombre"]?.toString() ?: "Cajero Principal"
            tvTicketFecha.text =
                "FECHA: ${header["fecha"]}\nMÉTODO: ${header["metodo_pago"]}\nCLIENTE: $cliente\nDOC: $doc\nLE ATENDIÓ: $vendedor"

            // 5. Renderizado de la lista de productos
            renderProducts(items)

            // 6. Cálculos de Impuestos (IGV y Gravada)
            val total = header["total"].toString().toDoubleOrNull() ?: 0.0
            val tasa = business["impuesto"]?.toString()?.toDoubleOrNull() ?: 18.0
            val subtotal = total / (1 + (tasa / 100))
            val igv = total - subtotal

            // 7. Inserción de Totales y Desglose
            // Agregamos una línea divisoria antes de los totales
            layoutProductItems.addView(createSeparator())

            // Contenedor para el bloque de totales (Forzamos WRAP_CONTENT)
            val layoutTotales = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            layoutTotales.addView(createTotalRow("OP. GRAVADA:", subtotal))
            layoutTotales.addView(createTotalRow("IGV (${tasa.toInt()}%):", igv))
            layoutTotales.addView(createTotalRow("TOTAL A PAGAR:", total, true))

            layoutProductItems.addView(layoutTotales)
            layoutProductItems.addView(createSeparator())


            // 8. Monto en Letras (Requisito Fiscal)
            val tvLetras = createTicketTextView("SON: ${convertirMontoALetras(total)}", 0).apply {
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 5, 0, 10)
            }
            layoutProductItems.addView(tvLetras)

            val tvLeyenda = TextView(context).apply {
                // Usamos el texto dinámico según el tipo de comprobante
                text =
                    "Representación impresa de la ${tipo.uppercase()} ELECTRÓNICA.\nConsulte su comprobante en: www.sunat.gob.pe"
                textSize = 9f
                setTextColor(android.graphics.Color.BLACK)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 10, 0, 10)

                // ESTA ES LA PARTE CLAVE: Forzamos que la altura sea solo la del texto
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Aseguramos que no tenga 'weight' que estire la vista
                    weight = 0f
                }
            }
            layoutProductItems.addView(tvLeyenda)

            // 9. QR - Posicionamiento Final
            if (idVenta.isNotEmpty()) {
                ivTicketQR.setImageBitmap(generateQRCode(idVenta))
                ivTicketQR.visibility = View.VISIBLE

                // Reajustamos los parámetros del QR para que no tenga 'weight' y suba
                val qrParams = ivTicketQR.layoutParams as LinearLayout.LayoutParams
                qrParams.height =
                    (140 * resources.displayMetrics.density).toInt() // Tamaño fijo 140dp
                qrParams.width = (140 * resources.displayMetrics.density).toInt()
                qrParams.weight = 0f
                qrParams.topMargin = 10
                ivTicketQR.layoutParams = qrParams
            }

            // --- 9. AGRADECIMIENTO Y CONTACTO (NUEVO - PROGRAMÁTICO) ---
            val telefono = business["telefono"]?.toString() ?: ""
            val celular = business["celular"]?.toString() ?: ""
            val contactoStr = if (celular.isNotEmpty()) "WhatsApp: $celular" else "Tel: $telefono"

            val footerText = "¡GRACIAS POR SU COMPRA!\n$contactoStr"

            layoutProductItems.addView(TextView(context).apply {
                text = footerText
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 15, 0, 15)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            })
        }
    }

    private fun createSeparator(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (2 * resources.displayMetrics.density).toInt() // 2dp de altura
            ).apply {
                val margin = (8 * resources.displayMetrics.density).toInt()
                setMargins(0, margin, 0, margin)
            }

            // Aplicamos el drawable entrecortado
            setBackgroundResource(com.kasolution.verify.R.drawable.line_dashed)

            // CLAVE: Forzar renderizado por software para que se vean los guiones
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun createTotalRow(label: String, value: Double, isBold: Boolean = false): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            weightSum = 10f
        }

        val tvLabel = createTicketTextView(label, 0, 7f, View.TEXT_ALIGNMENT_TEXT_END).apply {
            if (isBold) typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val tvVal = createTicketTextView(
            String.format(java.util.Locale.US, " S/ %.2f", value),
            0,
            3f,
            View.TEXT_ALIGNMENT_TEXT_END
        ).apply {
            if (isBold) typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        row.addView(tvLabel)
        row.addView(tvVal)
        return row
    }

    private fun convertirMontoALetras(monto: Double): String {
        val entero = monto.toLong()
        val centavos = ((monto - entero) * 100).toInt()
        return "${NumberToLetterConverter.convert(entero)} CON ${
            String.format(
                "%02d",
                centavos
            )
        }/100 SOLES"
    }

    private fun renderProducts(productos: List<Map<String, Any>>) {
//        binding.layoutProductItems.removeAllViews()

        productos.forEach { item ->
            val rowContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 10f // La suma total de la fila
                setPadding(0, 10, 0, 10)
            }

            // 1. CANTIDAD (Peso 1.0)
            val cant = item["cantidad"].toString().toDouble().toInt()
            val tvCant = createTicketTextView("${cant}x", 0, 1.2f)

            val um = item["unidad_medida"]?.toString() ?: "UND"
            val tvUm = createTicketTextView(um, 0, 1.5f)

            // 2. NOMBRE Y CÓDIGO (Peso 6.5)
            // Concatenamos el nombre y el código en un solo String para evitar anidar layouts
            val nombre = item["producto_nombre"]?.toString() ?: "Producto"
            val codigo = item["producto_codigo"]?.toString() ?: ""
            val textoAMostrar = if (codigo.isNotEmpty()) "$nombre\n[$codigo]" else nombre

            val tvDesc = createTicketTextView(textoAMostrar, 0, 4.8f).apply {
                // Permitimos que el texto use varias líneas si es largo
                textSize = 11f
                isSingleLine = false
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }

            // 3. IMPORTE (Peso 2.5)
            val precio = item["precio_unitario"].toString().toDouble()
            val tvImporte = createTicketTextView(
                String.format(java.util.Locale.US, "%.2f", cant * precio),
                0,
                2.5f,
                View.TEXT_ALIGNMENT_TEXT_END
            ).apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            rowContainer.addView(tvCant)
            rowContainer.addView(tvUm)
            rowContainer.addView(tvDesc)
            rowContainer.addView(tvImporte)

            binding.layoutProductItems.addView(rowContainer)
        }
    }

    private fun setupBottomSheetBehavior() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = true
            isHideable = true
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun createTicketTextView(
        text: String,
        widthDp: Int = 0,
        weight: Float = 0f,
        textAlign: Int = View.TEXT_ALIGNMENT_TEXT_START
    ): TextView {
        return TextView(context).apply {
            this.text = text
            this.textSize = 12f
            this.textAlignment = textAlign
            this.setTextColor(android.graphics.Color.BLACK)

            // Si hay peso, forzamos el ancho a 0 para que el peso mande
            val layoutWidth =
                if (weight > 0f) 0 else (widthDp * resources.displayMetrics.density).toInt()

            layoutParams = LinearLayout.LayoutParams(
                layoutWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
            )
        }
    }

    private fun generateQRCode(text: String): android.graphics.Bitmap? {
        val width = 400
        val height = 400
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        return try {
            val bitMatrix =
                writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, height)
            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.RGB_565
            )
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(
                                x,
                                y
                            )
                        ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generarPdfTicket(scrollView: NestedScrollView, nombreArchivo: String): File? {
        val childView = scrollView.getChildAt(0)

        // Definimos el margen (ejemplo: 20px). Puedes ajustarlo a tu gusto.
        val margin = 30

        // Calculamos las nuevas dimensiones incluyendo los márgenes
        val pdfWidth = childView.width + (margin * 2)
        val pdfHeight = childView.height + (margin * 2)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas

        // TRUCO: Movemos el origen del dibujo hacia la derecha y hacia abajo
        // Esto crea el efecto de margen blanco alrededor
        canvas.translate(margin.toFloat(), margin.toFloat())

        // Dibujamos la vista en la posición desplazada
        childView.draw(canvas)

        document.finishPage(page)

        return try {
            val file = File(requireContext().cacheDir, "$nombreArchivo.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compartirArchivo(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Comprobante de Pago")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Enviar Ticket por:"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IS_FROM_HISTORY = "is_from_history"

        // Método estático para abrirlo desde el Historial
        fun newInstance(isFromHistory: Boolean): SaleDetailSheet {
            val fragment = SaleDetailSheet()
            val args = Bundle()
            args.putBoolean(ARG_IS_FROM_HISTORY, isFromHistory)
            fragment.arguments = args
            return fragment
        }
    }
}