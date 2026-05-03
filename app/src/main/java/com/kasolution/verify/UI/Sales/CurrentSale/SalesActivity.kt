package com.kasolution.verify.UI.Sales.CurrentSale

import android.R.attr.duration
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Purchase.model.PurchaseItem
import com.kasolution.verify.UI.Sales.adapter.CartAdapter
import com.kasolution.verify.UI.Sales.adapter.SearchClienteAdapter
import com.kasolution.verify.UI.Sales.adapter.SearchProductAdapter
import com.kasolution.verify.UI.Sales.fragment.SaleDetailSheet
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivitySalesBinding
import com.kasolution.verify.databinding.DialogPagoBinding
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.UI.Sales.model.CartItem
import com.kasolution.verify.databinding.LayoutOtrosPagosBinding
import java.util.Locale

class SalesActivity : AppCompatActivity() {
    private val TAG = "SalesActivity"
    private lateinit var binding: ActivitySalesBinding
    private lateinit var adapterVenta: CartAdapter

    private var listaMaestra = listOf<Product>()
    private var listaClientes = listOf<Client>()
    private var idTipoComprobanteSeleccionado = 1

    private val viewModel: SalesViewModel by viewModels {
        AppProvider.provideSalesViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupScannerReceiver()
        setupBackPressedHandling()
        initialRecycler()
        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCobrar.setOnClickListener {
            // El total ahora viene del LiveData del ViewModel
            val total = viewModel.totalVenta.value ?: 0.0
            if (total > 0) {
                mostrarDialogoPago(total)
            }
        }
        binding.btnScannerVenta.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java).apply {
                putExtra("MULTI_SCAN", true)
                putExtra("INITIAL_TOTAL", viewModel.totalVenta.value ?: 0.0)
                putExtra("INITIAL_COUNT", viewModel.cartList.value?.sumOf { it.cantidad } ?: 0)
            }
            startActivity(intent)
        }
        binding.tilBuscarVenta.setEndIconOnClickListener {
            DialogHelper.showConfirmation(
                context = this,
                title = "¿Vaciar Carrito?",
                message = "Se eliminarán todos los productos agregados. ¿Deseas continuar?",
                onConfirm = {
                    viewModel.clearCart() // Tu función que ya limpia la lista y el total

                    ToastHelper.clasicCustomToast(binding.root, "Carro vacio", true)
                }
            )
        }
        binding.tilBuscarVenta.isEndIconVisible = false
    }

    private fun setupScannerReceiver() {
        val filter = IntentFilter().apply {
            addAction("ACTION_PRODUCT_SCANNED")
            addAction("ACTION_CLEAR_CART")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scannerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scannerReceiver, filter)
        }
    }

    private fun initialRecycler() {
// Conectamos el adaptador directamente a las funciones del ViewModel
        adapterVenta = CartAdapter(
            cartList = mutableListOf(),
            onIncrementClick = { item, _ ->
                viewModel.updateQuantity(item.producto.id, item.cantidad + 1)
            },
            onDecrementClick = { item, _ ->
                if (item.cantidad > 1) {
                    viewModel.updateQuantity(item.producto.id, item.cantidad - 1)
                } else {
                    confirmarEliminacion(item)
                }
            },
            onPriceEditClick = { item, _ ->
                showPriceDialog(item)
            },
            onDeleteClick = { position ->
                val item = viewModel.cartList.value?.getOrNull(position)
                item?.let { confirmarEliminacion(it) }
            },
            onQuantityClick = { cartitem, cantidad ->
                showQuantityDialog(cartitem, cantidad)
            }
        )

        binding.rvCarrito.apply {
            layoutManager = LinearLayoutManager(this@SalesActivity)
            adapter = adapterVenta
        }
    }

    private fun setupObservers() {
        // OBSERVADOR DEL CARRITO: Esta es la clave
        viewModel.cartList.observe(this) { nuevaLista ->
            val tieneProductos = nuevaLista.isNotEmpty()
            binding.tilBuscarVenta.isEndIconVisible = tieneProductos
            adapterVenta.updateList(nuevaLista) // Usa el DiffUtil que creamos
            viewModel.totalVenta.observe(this) { total ->
                binding.tvTotalPagar.text = String.format(Locale.US, "S/ %.2f", total)
            }

            // Actualizar el botón de cobrar
            binding.btnCobrar.isEnabled = nuevaLista.isNotEmpty()
            binding.btnCobrar.alpha = if (nuevaLista.isEmpty()) 0.5f else 1.0f
            actualizarTotalUI(nuevaLista)
        }

        viewModel.productsList.observe(this) { lista ->
            listaMaestra = lista
            setupBuscadorManual(lista)
        }

        viewModel.clientsList.observe(this) { lista ->
            listaClientes = lista
        }

        viewModel.operationSuccess.observe(this) { accion ->
            if (accion == "SALE_SAVE") {
                viewModel.resetOperationStatus()
                mostrarTicket()
                //ToastHelper.showCustomToast(binding.root, "Venta realizada con éxito", true)
            }
        }


        viewModel.totalVenta.observe(this) { total ->
            Log.d(TAG, "Total a pagar actualizado: $total")
            // Aquí actualizamos el TextView de la parte inferior de la Activity
            binding.tvTotalPagar.text = String.Companion.format(Locale.US, "S/ %.2f", total)

            // También aprovechamos para habilitar/deshabilitar el botón de cobrar
            binding.btnCobrar.isEnabled = total > 0
            binding.btnCobrar.alpha = if (total > 0) 1.0f else 0.5f
        }

        viewModel.exception.observe(this) { error ->
            ToastHelper.showCustomToast(binding.root, error, false)
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading) ProgressHelper.showProgress(this, "Cargando productos...")
            else ProgressHelper.hideProgress()
        }
    }

    private fun showPriceDialog(item: CartItem) {
        DialogHelper.showPriceDialog(
            context = this,
            productName = item.producto.nombre,
            currentPrice = item.producto.precioVenta,
            colorRes = R.color.blue_clients
        ) { nuevoPrecio ->
            // Este bloque (lambda) se ejecuta solo cuando el usuario da a "ACTUALIZAR"
            viewModel.updatePrice(item.producto.id, nuevoPrecio)
        }
    }

    private fun showQuantityDialog(item: CartItem, position: Int) {
        DialogHelper.showQuantityDialog(
            context = this,
            productName = item.producto.nombre,
            currentQuantity = item.cantidad,
            colorRes = R.color.blue_clients
        ) { nuevaCantidad ->
            viewModel.updateQuantity(item.producto.id, nuevaCantidad)
        }
    }

    private fun mostrarTicket() {
        val ticketSheet = SaleDetailSheet()
        // Usamos supportFragmentManager porque estamos en una Activity
        ticketSheet.show(supportFragmentManager, "SaleDetailSheet")
    }

    private fun setupBuscadorManual(listaProductos: List<Product>) {
        val searchAdapter = SearchProductAdapter(this, listaProductos)
        binding.etBuscarVenta.apply {
            setAdapter(searchAdapter)
            setOnItemClickListener { parent, _, position, _ ->
                val productoSeleccionado = parent.getItemAtPosition(position) as Product
                viewModel.addProductToCart(productoSeleccionado)
                post {
                    setText("", false)
                    requestFocus()
                }
            }
        }
    }


    private fun confirmarEliminacion(item: CartItem) {
        DialogHelper.showConfirmation(
            context = this,
            title = "Eliminar Producto",
            message = "¿Deseas quitar ${item.producto.nombre}?",
            onConfirm = {
                viewModel.removeItem(item.producto.id)
            }
        )
    }

    private fun actualizarTotalUI(lista: List<CartItem>) {
        val total = lista.sumOf { it.subtotal }
        binding.tvTotalPagar.text = String.Companion.format(Locale.US, "S/ %.2f", total)
        binding.btnCobrar.isEnabled = lista.isNotEmpty()
        binding.btnCobrar.alpha = if (lista.isEmpty()) 0.5f else 1.0f
    }

    private fun mostrarDialogoPago(total: Double) {
        var clienteDelDialogo: Client? = null
        idTipoComprobanteSeleccionado = 1
        var otrosPagos: Map<Int, Double> = emptyMap()
        val binding = DialogPagoBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, binding.root)

        // --- 1. CONFIGURAR BUSCADOR DE CLIENTES ---
        val clienteAdapter = SearchClienteAdapter(this, listaClientes)
        binding.actvBuscarCliente.apply {
            setAdapter(clienteAdapter)
            setOnItemClickListener { parent, _, position, _ ->
                val cliente = parent.getItemAtPosition(position) as Client
                clienteDelDialogo = cliente
                setText(cliente.dniRuc, false)
                if (idTipoComprobanteSeleccionado == 2) {
                    binding.etRazonSocial.setText(cliente.nombre)
                    binding.tilRazonSocial.error = null
                }
                hideKeyboard()
                binding.tilBuscarCliente.error = null
            }
        }

        // --- 2. SELECCIÓN DE COMPROBANTE ---
        binding.toggleTipoComprobante.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
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

        // --- 3. LÓGICA DE MONTOS Y VALIDACIÓN ---
        binding.tvTotalDialogo.text = String.format(Locale.US, "S/ %.2f", total)

        fun validarPago() {
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
                        binding.tvVuelto.text =
                            String.format(Locale.US, "Falta: S/ %.2f", Math.abs(vuelto))
                        binding.tvVuelto.setTextColor(Color.RED)
                        binding.btnConfirmarPago.isEnabled = false
                    }
                }

                binding.btnOtros.id -> {
                    pagosOtros(total) { pagos ->
                        Log.i("pagosOtros", "pagos: $pagos")
                        otrosPagos=pagos
                    }


//                    if (montoRecibido > 0 && montoRecibido < total) {
//                        val aTarjeta = total - montoRecibido
//                        binding.tvVuelto.text = String.format(Locale.US, "Tarjeta: S/ %.2f", aTarjeta)
//                        binding.tvVuelto.setTextColor(Color.BLUE)
//                        binding.btnConfirmarPago.isEnabled = true
//                    } else {
//                        binding.tvVuelto.text = "Monto efectivo debe ser menor al total"
//                        binding.tvVuelto.setTextColor(Color.RED)
//                        binding.btnConfirmarPago.isEnabled = false
//                    }
                }
            }
        }

        // Listeners de montos y métodos
        binding.etMontoRecibido.addTextChangedListener { validarPago() }
        binding.toggleTipoComprobante.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // Esto prepara una animación de transición para cualquier cambio
                // que ocurra en el layout (como el cambio de color de fondo)
                val transition = AutoTransition().apply {
                    duration = 250 // Milisegundos
                }
                TransitionManager.beginDelayedTransition(group, transition)
            }
        }

        binding.toggleMetodoPago.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // El panel de efectivo se muestra para Efectivo o Mixto
                binding.containerEfectivo.visibility =
                    if (checkedId == binding.btnTarjeta.id) View.GONE else View.VISIBLE

                // Ajustar hint según el modo
                binding.tilMontoRecibido.hint =
                    if (checkedId == binding.btnOtros.id) "Efectivo a recibir" else "Monto Recibido"

                validarPago()
            }
        }


        // Botones rápidos
        fun setMontoRapido(monto: Double) {
            binding.etMontoRecibido.setText(String.format(Locale.US, "%.2f", monto))
            binding.etMontoRecibido.setSelection(binding.etMontoRecibido.text?.length ?: 0)
            validarPago()
        }
        binding.btnExacto.setOnClickListener { setMontoRapido(total) }
        binding.btnMonto10.setOnClickListener { setMontoRapido(10.0) }
        binding.btnMonto20.setOnClickListener { setMontoRapido(20.0) }
        binding.btnMonto50.setOnClickListener { setMontoRapido(50.0) }
        binding.btnMonto100.setOnClickListener { setMontoRapido(100.0) }

        // --- 4. FINALIZAR VENTA ---
        binding.btnConfirmarPago.setOnClickListener {
            val ruc = binding.actvBuscarCliente.text.toString()
            val razonSocial = binding.etRazonSocial.text.toString()

            if (idTipoComprobanteSeleccionado == 2) {
                if (ruc.length != 11) {
                    binding.tilBuscarCliente.error = "RUC inválido"; return@setOnClickListener
                }
                if (razonSocial.isEmpty()) {
                    binding.tilRazonSocial.error = "Requerido"; return@setOnClickListener
                }
            }

            // Preparar lista de pagos para el backend
            val pagos = mutableListOf<Map<String, Any>>()
            val montoRecibido = binding.etMontoRecibido.text.toString().toDoubleOrNull() ?: 0.0

            when (binding.toggleMetodoPago.checkedButtonId) {
                binding.btnEfectivo.id -> pagos.add(mapOf("metodo" to "EFECTIVO", "monto" to total))
                binding.btnTarjeta.id -> pagos.add(mapOf("metodo" to "TARJETA", "monto" to total))
                binding.btnOtros.id -> {

                    if (otrosPagos.size==1) {
                        val id = otrosPagos.keys.first()
                        pagos.add(mapOf("metodo" to obtenerNombrePorId(id), "monto" to total))
                    }else{
                        for ((id, monto) in otrosPagos) {
                            pagos.add(mapOf("metodo" to obtenerNombrePorId(id), "monto" to monto))
                        }
                    }

//                    pagos.add(mapOf("metodo" to "EFECTIVO", "monto" to montoRecibido))
//                    pagos.add(mapOf("metodo" to "TARJETA", "monto" to (total - montoRecibido)))
                }
            }

            viewModel.saveSale(
                idCliente = clienteDelDialogo?.id,
                idEmpleado = viewModel.userId,
                pagos = pagos,
                idTipoComprobante = idTipoComprobanteSeleccionado
            )
            dialog.dismiss()
        }

        binding.btnCancelarPago.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    fun pagosOtros(total: Double,onResult: (Map<Int, Double>) -> Unit) {
        val binding = LayoutOtrosPagosBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, binding.root)
        var cantSeleccionados = 0
        // Mapeamos los Checkbox con sus respectivos Inputs e IDs
        val controles = listOf(
            Triple(binding.cbEfectivo, binding.tilEfectivo, 1),
            Triple(binding.cbTarjeta, binding.tilTarjeta, 2),
            Triple(binding.cbTransferencia, binding.tilTransferencia, 3),
            Triple(binding.cbYape, binding.tilYape, 4),
            Triple(binding.cbPlin, binding.tilPlin, 5)
        )

        controles.forEach { (checkbox, inputLayout, _) ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (cantSeleccionados < 2) {
                        cantSeleccionados++
                        inputLayout.isEnabled = true
                    } else {
                        // Bloqueo preventivo: No deja marcar el tercero
                        checkbox.isChecked = false
                        Toast.makeText(this, "Solo se permite seleccionar 2 métodos de pago", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    cantSeleccionados--
                    inputLayout.isEnabled = false
                    inputLayout.editText?.text?.clear()
                }
            }
        }
        binding.btnQuickYape.setOnClickListener { onResult(mapOf(4 to 0.0)); dialog.dismiss() }
        binding.btnQuickPlin.setOnClickListener { onResult(mapOf(5 to 0.0)); dialog.dismiss() }
        binding.btnQuickTransf.setOnClickListener { onResult(mapOf(3 to 0.0)); dialog.dismiss() }

        binding.btnShowMixto.setOnClickListener {
            binding.containerQuickButtons.visibility = View.GONE
            binding.containerMixtoFields.visibility = View.VISIBLE
            binding.tvTituloOtros.text = "Configurar Pago Mixto"
            binding.tvSaldoPagar.text = "Total a pagar: S/ $total"

        }
        binding.btnClose.setOnClickListener { dialog.dismiss() }
        binding.btnConfirmarMixto.setOnClickListener {
            if (cantSeleccionados != 2) {
                Toast.makeText(this, "Debe seleccionar exactamente 2 métodos", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Ingrese los montos para ambos métodos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. Validar suma exacta
            val sumaIngresada = seleccionados.values.sum()
            if (Math.abs(sumaIngresada - total) > 0.01) {
                Toast.makeText(this, "La suma (S/ $sumaIngresada) no coincide con el total (S/ $total)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Si todo OK, enviamos el mismo Map de siempre
            onResult(seleccionados)
            dialog.dismiss()
        }
        dialog.show()
    }

    private val scannerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_PRODUCT_SCANNED" -> {
                    val codigoSaneado = intent.getStringExtra("SCAN_RESULT_CODE") ?: return
                    // Buscamos en el LiveData del VM directamente
                    val producto = viewModel.productsList.value?.find { it.codigo == codigoSaneado }

                    if (producto != null) {
                        // USAR postValue o runOnUiThread es vital aquí
                        viewModel.addProductToCart(producto)
                        Log.d(
                            "SCANNER_DEBUG",
                            "Producto encontrado y enviado al VM: ${producto.nombre}"
                        )
                    } else {
                        Log.e("SCANNER_DEBUG", "Código $codigoSaneado no existe en la lista del VM")
                    }
                }

                "ACTION_CLEAR_CART" -> {
                    Log.d("SCANNER_DEBUG", "Recibido ACTION_CLEAR_CART")
                    viewModel.clearCart()
                    ToastHelper.clasicCustomToast(binding.root, "Carrito vacio", true)
                }
            }
        }
    }

    private fun setupBackPressedHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Obtenemos la lista actual del ViewModel
                val carrito = viewModel.cartList.value

                // Si el carrito no es nulo y tiene elementos, preguntamos
                if (!carrito.isNullOrEmpty()) {
                    DialogHelper.showConfirmation(
                        context = this@SalesActivity,
                        title = "Confirmar salida",
                        message = "Tienes productos en el carrito. ¿Estás seguro de que deseas abandonar la venta actual?",
                        onConfirm = {
                            // Si confirma, cerramos la actividad
                            finish()
                        },
                        onCancel = {
                            // Si cancela, no hacemos nada (el diálogo se cierra solo por tu helper)
                            Log.d("SalesActivity", "Salida cancelada por el usuario")
                        }
                    )
                } else {
                    // Si el carrito está vacío, salimos directamente
                    isEnabled = false // Desactivamos el callback para no entrar en bucle
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }


    private fun View.hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(scannerReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar: ${e.message}")
        }
    }
}