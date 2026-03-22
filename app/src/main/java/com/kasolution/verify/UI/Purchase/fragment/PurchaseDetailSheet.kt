package com.kasolution.verify.UI.Purchase.fragment

import android.app.Dialog
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
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kasolution.verify.R
import com.kasolution.verify.UI.Purchase.history.PurchaseHistoryActivity
import com.kasolution.verify.UI.Purchase.viewModel.PurchaseViewModel
import com.kasolution.verify.databinding.LayoutTicketSheetBinding
import java.io.File
import java.io.FileOutputStream

class PurchaseDetailSheet : BottomSheetDialogFragment() {
    private val TAG = "PurchaseDetailSheet"
    private var _binding: LayoutTicketSheetBinding? = null
    private val binding get() = _binding!!
    private var isFromHistory: Boolean = false

    private val viewModel: PurchaseViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                // 1. Hacemos el contenedor del sistema transparente para el efecto flotante
                it.setBackgroundColor(Color.TRANSPARENT)
                // 2. Quitamos la sombra del sistema para usar la de nuestra CardView
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

        binding.btnAnularVenta.text = "ANULAR"
        binding.btnAnularVenta.visibility = if (isFromHistory) View.VISIBLE else View.GONE

        viewModel.purchaseDetailData.observe(viewLifecycleOwner) { data ->
            data?.let { renderPurchaseInvoice(it) }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { action ->
            if (action == "PURCHASE_DELETE") dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.Animation_iOS_Sheet)
    }

    private fun renderPurchaseInvoice(data: Map<String, Any>) {

        val business = data["business"] as? Map<String, Any> ?: emptyMap()
        val header = data["header"] as? Map<String, Any> ?: emptyMap()
        val items = data["items"] as? List<Map<String, Any>> ?: emptyList()

        val estado = header["estado"]?.toString() ?: "COMPLETADO"
        val esAnulada = estado.equals("ANULADO", ignoreCase = true)

        with(binding) {
            layoutProductItems.removeAllViews()
            ivTicketQR.isVisible = false

            if (esAnulada) {
                btnAnularVenta.isVisible = false
                btnImprimirDuplicado.isVisible = false
                ivSelloAnulado.isVisible = true
            } else {
                btnAnularVenta.isVisible = isFromHistory
                btnImprimirDuplicado.isVisible = true
                ivSelloAnulado.isVisible = false
            }

            val idCompra = header["id_compra"]?.toString() ?: ""

            btnAnularVenta.setOnClickListener {
                if (idCompra.isNotEmpty()) {
                    (requireActivity() as? PurchaseHistoryActivity)?.confirmAnnul(idCompra.toDouble().toInt())
                }
            }

            btnImprimirDuplicado.setOnClickListener {
                val pdfFile = generarPdfTicket(scrollTicket, "Compra_$idCompra")
                pdfFile?.let { compartirArchivo(it) }
            }

            // Datos del Negocio (Ajustado a las llaves que vienen de PHP)
            val nombreEmpresa = business["nombre_negocio"] ?: "MI NEGOCIO"
            val direccion = business["direccion"] ?: ""
            val ruc = business["ruc"] ?: ""
            val telefono = business["telefono"] ?: ""

            tvTicketId.text = "$nombreEmpresa\n$direccion\nRuc: $ruc\nTel: $telefono"

            tvTicketComprobanteNro.text = "ORDEN DE COMPRA\nN° ${idCompra.padStart(6, '0')}"

            val proveedor = header["proveedor_nombre"]?.toString() ?: "PROVEEDOR DESCONOCIDO"
            val empleado = header["empleado_nombre"]?.toString() ?: "Usuario"
            tvTicketFecha.text = "FECHA: ${header["fecha"]}\nPROVEEDOR: $proveedor\nRECIBIDO POR: $empleado"

            renderItems(items)

            val total = header["total"].toString().toDoubleOrNull() ?: 0.0
            layoutProductItems.addView(createSeparator())
            layoutProductItems.addView(createTotalRow("TOTAL COMPRA:", total, true))

            layoutProductItems.addView(TextView(context).apply {
                text = "\n*** Documento de Control Interno ***"
                textSize = 10f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            })
        }
    }

    private fun renderItems(productos: List<Map<String, Any>>) {
        productos.forEach { item ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2)
                weightSum = 10f
                setPadding(0, 8, 0, 8)
            }

            val cant = item["cantidad"].toString().toDouble().toInt()
            val nombre = item["producto_nombre"]?.toString() ?: "Producto"
            val precio = item["precio_compra"].toString().toDouble()

            row.addView(createTicketTextView("${cant}x", 0, 1.5f))
            row.addView(createTicketTextView(nombre, 0, 6f).apply { isSingleLine = false })
            row.addView(createTicketTextView(
                String.format("%.2f", cant * precio), 0, 2.5f, View.TEXT_ALIGNMENT_TEXT_END
            ).apply { setTypeface(null, Typeface.BOLD) })

            binding.layoutProductItems.addView(row)
        }
    }

    private fun createSeparator(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(-1, (2 * resources.displayMetrics.density).toInt()).apply {
            setMargins(0, 10, 0, 10)
        }
        setBackgroundResource(R.drawable.line_dashed)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private fun createTotalRow(label: String, value: Double, isBold: Boolean): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 10f }
        row.addView(createTicketTextView(label, 0, 7f, View.TEXT_ALIGNMENT_TEXT_END).apply {
            if (isBold) typeface = Typeface.DEFAULT_BOLD
        })
        row.addView(createTicketTextView(String.format(" S/ %.2f", value), 0, 3f, View.TEXT_ALIGNMENT_TEXT_END).apply {
            if (isBold) typeface = Typeface.DEFAULT_BOLD
        })
        return row
    }

    private fun createTicketTextView(t: String, w: Int, wg: Float, align: Int = View.TEXT_ALIGNMENT_TEXT_START) =
        TextView(context).apply {
            text = t; textSize = 11f; textAlignment = align; setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(if (wg > 0) 0 else w, -2, wg)
        }

    private fun setupBottomSheetBehavior() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = true // Habilita el cierre deslizando desde el Handle
        }
    }

    private fun generarPdfTicket(scrollView: NestedScrollView, nombre: String): File? {
        val child = scrollView.getChildAt(0)
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(child.width + 60, child.height + 60, 1).create()
        val page = doc.startPage(info)
        page.canvas.translate(30f, 30f)
        child.draw(page.canvas)
        doc.finishPage(page)
        val file = File(requireContext().cacheDir, "$nombre.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

    private fun compartirArchivo(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir Orden de Compra"))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val ARG_IS_FROM_HISTORY = "is_from_history"
        fun newInstance(isFromHistory: Boolean) = PurchaseDetailSheet().apply {
            arguments = Bundle().apply { putBoolean(ARG_IS_FROM_HISTORY, isFromHistory) }
        }
    }
}