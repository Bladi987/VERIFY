package com.kasolution.verify.UI.Sales.fragment

import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.kasolution.verify.R
import com.kasolution.verify.UI.Sales.adapter.SearchClienteAdapter
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.core.utils.setupCurrencyFormatting
import com.kasolution.verify.databinding.DialogPagoBinding
import com.kasolution.verify.databinding.LayoutOtrosPagosBinding
import com.kasolution.verify.domain.clients.model.Client
import java.util.Locale
import kotlin.math.abs

class PagoDialogFragment : DialogFragment() {
    private var _binding: DialogPagoBinding? = null
    private val binding get() = _binding!!

    // Estado del diálogo
    private var total: Double = 0.0
    private var listaClientes: List<Client> = emptyList()
    private var clienteSeleccionado: Client? = null
    private var idTipoComprobanteSeleccionado = 1
    private var otrosPagos: Map<Int, Double> = emptyMap()

    // Callback para devolver los datos al Activity/ViewModel
    private var onConfirmar: ((idCliente: Int?, idTipoComprobante: Int, pagos: List<Map<String, Any>>) -> Unit)? =
        null

    companion object {
        fun newInstance(
            total: Double,
            clientes: List<Client>,
            callback: (Int?, Int, List<Map<String, Any>>) -> Unit
        ): PagoDialogFragment {
            return PagoDialogFragment().apply {
                this.total = total
                this.listaClientes = clientes
                this.onConfirmar = callback
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPagoBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(requireContext(), binding.root)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBuscadorClientes()
        setupSeleccionComprobante()
        setupLogicaMontos()
        setupMetodosPago()
        setupBotonesRapidos()

        binding.btnCancelarPago.setOnClickListener { dismiss() }
    }

    // --- 1. BUSCADOR DE CLIENTES ---
    private fun setupBuscadorClientes() {
        val clienteAdapter = SearchClienteAdapter(requireContext(), listaClientes)
        binding.actvBuscarCliente.apply {
            setAdapter(clienteAdapter)
            setOnItemClickListener { parent, _, position, _ ->
                val cliente = parent.getItemAtPosition(position) as Client
                clienteSeleccionado = cliente
                setText(cliente.dniRuc, false)
                if (idTipoComprobanteSeleccionado == 2) {
                    binding.etRazonSocial.setText(cliente.nombre)
                    binding.tilRazonSocial.error = null
                }
                hideKeyboard() // Asegúrate de tener esta extensión o función accesible
                binding.tilBuscarCliente.error = null
            }
        }
    }

    // --- 2. SELECCIÓN DE COMPROBANTE ---
    private fun setupSeleccionComprobante() {
        binding.toggleTipoComprobante.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // Animación de transición
                TransitionManager.beginDelayedTransition(
                    group,
                    AutoTransition().apply { duration = 250 })

                when (checkedId) {
                    binding.btnBoleta.id -> {
                        idTipoComprobanteSeleccionado = 1
                        binding.tilBuscarCliente.hint = "DNI / Cliente"
                        binding.tilRazonSocial.visibility = View.GONE
                    }

                    binding.btnFactura.id -> {
                        idTipoComprobanteSeleccionado = 2
                        binding.tilBuscarCliente.hint = "RUC (11 dígitos)"
                        binding.tilRazonSocial.visibility = View.VISIBLE
                    }

                    binding.btnTicket.id -> {
                        idTipoComprobanteSeleccionado = 3
                        binding.tilBuscarCliente.hint = "Cliente (Opcional)"
                        binding.tilRazonSocial.visibility = View.GONE
                    }
                }
            }
        }
    }

    // --- 3. LÓGICA DE MONTOS Y VALIDACIÓN ---
    private fun setupLogicaMontos() {
        binding.tvTotalDialogo.text = String.format(Locale.US, "S/ %.2f", total)
        binding.etMontoRecibido.addTextChangedListener { validarPago() }
    }

    private fun validarPago() {
        val montoRecibido = binding.etMontoRecibido.text.toString().toDoubleOrNull() ?: 0.0
        val metodoSeleccionado = binding.toggleMetodoPago.checkedButtonId

        when (metodoSeleccionado) {
            binding.btnTarjeta.id -> {
                binding.tvVuelto.text = "Cargo total a tarjeta"
                binding.tvVuelto.setTextColor(Color.GRAY)
                binding.btnConfirmarPago.isEnabled = true
            }

            binding.btnEfectivo.id -> {
                val vuelto = montoRecibido - total
                if (vuelto >= 0) {
                    binding.tvVuelto.text = String.format(Locale.US, "Vuelto: S/ %.2f", vuelto)
                    binding.tvVuelto.setTextColor(Color.parseColor("#4CAF50"))
                    binding.btnConfirmarPago.isEnabled = true
                } else {
                    binding.tvVuelto.text = String.format(Locale.US, "Falta: S/ %.2f", abs(vuelto))
                    binding.tvVuelto.setTextColor(Color.RED)
                    binding.btnConfirmarPago.isEnabled = false
                }
            }

            binding.btnOtros.id -> {
                // Si es "Otros", la validez depende de si el mapa otrosPagos está lleno
                binding.btnConfirmarPago.isEnabled = otrosPagos.isNotEmpty()
            }
        }
    }

    private fun setupMetodosPago() {
        binding.toggleMetodoPago.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                binding.containerEfectivo.visibility =
                    if (checkedId == binding.btnTarjeta.id || checkedId == binding.btnOtros.id) View.GONE else View.VISIBLE
//                binding.btnConfirmarPago.isEnabled =
//                    if (checkedId == binding.btnTarjeta.id || checkedId == binding.btnOtros.id) true else false
                binding.tilMontoRecibido.hint =
                    if (checkedId == binding.btnOtros.id) "Efectivo a recibir" else "Monto Recibido"

                if (checkedId == binding.btnOtros.id) {
                    abrirDialogoOtrosPagos()
                }
                validarPago()
            }
        }

        binding.btnConfirmarPago.setOnClickListener { procesarVentaFinal() }
    }

    private fun abrirDialogoOtrosPagos() {
        pagosOtros(total) { pagos ->
            Log.i("pagosOtros", "pagos: $pagos")
            otrosPagos = pagos
            validarPago()
        }


//        (requireActivity() as? SalesActivity)?.pagosOtros(total) { pagos ->
//            this.otrosPagos = pagos
//            validarPago()
//        }

    }

    // --- 4. BOTONES RÁPIDOS ---
    private fun setupBotonesRapidos() {
        fun setMonto(monto: Double) {
            binding.etMontoRecibido.setText(String.format(Locale.US, "%.2f", monto))
            binding.etMontoRecibido.setSelection(binding.etMontoRecibido.text?.length ?: 0)
            validarPago()
        }
        binding.btnExacto.setOnClickListener { setMonto(total) }
        binding.btnMonto10.setOnClickListener { setMonto(10.0) }
        binding.btnMonto20.setOnClickListener { setMonto(20.0) }
        binding.btnMonto50.setOnClickListener { setMonto(50.0) }
        binding.btnMonto100.setOnClickListener { setMonto(100.0) }
    }

    // --- FINALIZACIÓN ---
    private fun procesarVentaFinal() {
        val ruc = binding.actvBuscarCliente.text.toString()
        val razonSocial = binding.etRazonSocial.text.toString()

        if (idTipoComprobanteSeleccionado == 2) {
            if (ruc.length != 11) {
                binding.tilBuscarCliente.error = "RUC inválido"; return
            }
            if (razonSocial.isEmpty()) {
                binding.tilRazonSocial.error = "Requerido"; return
            }
        }

        val pagos = mutableListOf<Map<String, Any>>()
        when (binding.toggleMetodoPago.checkedButtonId) {
            binding.btnEfectivo.id -> pagos.add(mapOf("metodo" to "EFECTIVO", "monto" to total))
            binding.btnTarjeta.id -> pagos.add(mapOf("metodo" to "TARJETA", "monto" to total))
            binding.btnOtros.id -> {
                if (otrosPagos.size == 1) {
                    val id = otrosPagos.keys.first()
                    pagos.add(mapOf("metodo" to obtenerNombrePorId(id), "monto" to total))
                } else {
                    otrosPagos.forEach { (id, monto) ->
                        pagos.add(mapOf("metodo" to obtenerNombrePorId(id), "monto" to monto))
                    }
                }
            }
        }

        // Ejecutar el callback que definimos en el Activity
        onConfirmar?.invoke(clienteSeleccionado?.id, idTipoComprobanteSeleccionado, pagos)
        dismiss()
    }


    private fun obtenerNombrePorId(id: Int): String {
        return when (id) {
            1 -> "EFECTIVO"
            2 -> "TARJETA"
            3 -> "TRANSFERENCIA"
            4 -> "YAPE"
            5 -> "PLIN"
            else -> "OTRO"
        }
    }

    fun pagosOtros(total: Double, onResult: (Map<Int, Double>) -> Unit) {
        val binding = LayoutOtrosPagosBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(requireContext(), binding.root)
        var cantSeleccionados = 0
        var mostrarMas = false
        binding.btnConfirmarMixto.isEnabled = false
        binding.etEfectivo.setupCurrencyFormatting()
        binding.etTarjeta.setupCurrencyFormatting()
        binding.etTransferencia.setupCurrencyFormatting()
        binding.etYape.setupCurrencyFormatting()
        binding.etPlin.setupCurrencyFormatting()


        // Mapeamos los Checkbox con sus respectivos Inputs e IDs
        val controles = listOf(
            Triple(binding.cbEfectivo, binding.tilEfectivo, 1),
            Triple(binding.cbTarjeta, binding.tilTarjeta, 2),
            Triple(binding.cbTransferencia, binding.tilTransferencia, 3),
            Triple(binding.cbYape, binding.tilYape, 4),
            Triple(binding.cbPlin, binding.tilPlin, 5)
        )

        fun actualizarSaldoPendiente() {
            var sumaActual = 0.0
            controles.forEach { (cb, til, _) ->
                if (cb.isChecked) {
                    sumaActual += til.editText?.text.toString().toDoubleOrNull() ?: 0.0
                }
            }

            // Usamos un margen de error mínimo para evitar problemas con decimales (Epsilon)
            val diferencia = total - sumaActual
            val esMontoExacto = Math.abs(diferencia) < 0.001

            // Actualización visual del mensaje de saldo
            when {
                esMontoExacto -> {
                    binding.tvSaldoMixto.text = "¡Saldo Completo!"
                    binding.tvSaldoMixto.setTextColor(Color.parseColor("#4CAF50")) // Verde
                }

                diferencia > 0 -> {
                    binding.tvSaldoMixto.text = "Faltan: S/ ${String.format("%.2f", diferencia)}"
                    binding.tvSaldoMixto.setTextColor(Color.RED)
                }

                else -> {
                    binding.tvSaldoMixto.text =
                        "Sobra: S/ ${String.format("%.2f", Math.abs(diferencia))}"
                    binding.tvSaldoMixto.setTextColor(Color.parseColor("#FF9800")) // Naranja/Ambar
                }
            }

            // REGLA DE ORO: El botón solo se habilita si el monto es exacto y hay 2 seleccionados
            binding.btnConfirmarMixto.isEnabled = esMontoExacto && cantSeleccionados == 2

            // Opcional: Cambiar la opacidad del botón para que se note más el bloqueo
            binding.btnConfirmarMixto.alpha =
                if (binding.btnConfirmarMixto.isEnabled) 1.0f else 0.5f
        }
        controles.forEach { (checkbox, inputLayout, _) ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (cantSeleccionados < 2) {
                        cantSeleccionados++
                        inputLayout.isEnabled = true
                    } else {
                        // Bloqueo preventivo: No deja marcar el tercero
                        checkbox.isChecked = false
//                        Toast.makeText(this, "Solo se permite seleccionar 2 métodos de pago", Toast.LENGTH_SHORT).show()
                        ToastHelper.showCustomToast(
                            binding.root,
                            "Solo se permite seleccionar 2 métodos de pago",
                            false
                        )
                    }
                } else {
                    cantSeleccionados--
                    inputLayout.isEnabled = false
                    inputLayout.editText?.text?.clear()
                }
                actualizarSaldoPendiente()
            }
            inputLayout.editText?.addTextChangedListener { actualizarSaldoPendiente() }
        }
        binding.btnQuickYape.setOnClickListener { onResult(mapOf(4 to 0.0)); dialog.dismiss() }
        binding.btnQuickPlin.setOnClickListener { onResult(mapOf(5 to 0.0)); dialog.dismiss() }
        binding.btnQuickTransf.setOnClickListener { onResult(mapOf(3 to 0.0)); dialog.dismiss() }
        binding.btnClose.setOnClickListener {
            if (!mostrarMas) {
                dialog.dismiss()
            } else {
                mostrarMas = false
                binding.btnClose.setImageResource(R.drawable.ic_close)
                binding.containerQuickButtons.visibility = View.VISIBLE
                binding.containerMixtoFields.visibility = View.GONE
            }
        }

        binding.btnShowMixto.setOnClickListener {
            mostrarMas = true
            binding.btnClose.setImageResource(R.drawable.ic_arrow_black)
            binding.containerQuickButtons.visibility = View.GONE
            binding.containerMixtoFields.visibility = View.VISIBLE
            binding.tvTituloOtros.text = "Configurar Pago Mixto"
            binding.tvSaldoPagar.text =
                "Total a pagar: S/ ${String.format(Locale.US, "%.2f", total)}"
            actualizarSaldoPendiente()

        }
        binding.btnConfirmarMixto.setOnClickListener {
            if (cantSeleccionados != 2) {
                //Toast.makeText(this, "Debe seleccionar exactamente 2 métodos", Toast.LENGTH_SHORT).show()
                ToastHelper.showCustomToast(
                    binding.root,
                    "Debe seleccionar exactamente 2 métodos",
                    false
                )
                return@setOnClickListener
            }
            val seleccionados = mutableMapOf<Int, Double>()

            // 2. Recolectar datos
            controles.forEach { (checkbox, inputLayout, id) ->
                if (checkbox.isChecked) {
                    val monto = inputLayout.editText?.text.toString().toDoubleOrNull() ?: 0.0
                    if (monto > 0) {
                        seleccionados[id] = monto
                    }
                }
            }

            // 3. Verificar que se recolectaron los 2 montos (que no haya vacíos)
            if (seleccionados.size != 2) {
                Toast.makeText(
                    requireContext(),
                    "Ingrese los montos para ambos métodos",
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@setOnClickListener
            }

            // 4. Validar suma exacta
            val sumaIngresada = seleccionados.values.sum()
            if (Math.abs(sumaIngresada - total) > 0.01) {
                ToastHelper.clasicCustomToast(
                    binding.root,
                    "La suma (S/ $sumaIngresada) no coincide con el total (S/ $total)",
                    false
                )
                return@setOnClickListener
            }

            onResult(seleccionados)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}