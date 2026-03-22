package com.kasolution.verify.UI.Sales.CurrentSale

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
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
                ToastHelper.showCustomToast(binding.root, "Venta realizada con éxito", true)
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
        val binding = DialogPagoBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, binding.root)


        // 1. Configurar Buscador de Clientes
        val clienteAdapter = SearchClienteAdapter(this, listaClientes)
        binding.actvBuscarCliente.apply {
            setAdapter(clienteAdapter)
            setOnItemClickListener { parent, _, position, _ ->

                val cliente = parent.getItemAtPosition(position) as Client
                clienteDelDialogo = cliente
                // Si el cliente seleccionado tiene razón social y es factura, podrías autocompletar
                if (idTipoComprobanteSeleccionado == 2) {
                    // 1. Forzamos que el campo de búsqueda mantenga el número (DNI/RUC)
                    // Usamos 'false' para que no vuelva a filtrar la lista desplegable
                    setText(cliente.dniRuc, false)

                    // 2. Llenamos automáticamente la Razón Social con el nombre del cliente
                    binding.etRazonSocial.setText(cliente.nombre)

                    // 3. Limpiamos errores visuales si existían
                    binding.tilBuscarCliente.error = null
                    binding.tilRazonSocial.error = null
                } else {
                    // Para Boleta o Ticket, también mantenemos el número en el campo
                    setText(cliente.dniRuc, false)
                }
                hideKeyboard()
                binding.tilBuscarCliente.error = null
            }
        }

        // 2. Lógica de Selección de Comprobante (NUEVO)
        binding.toggleTipoComprobante.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnBoleta.id -> {
                        idTipoComprobanteSeleccionado = 1
                        binding.actvBuscarCliente.inputType =
                            android.text.InputType.TYPE_CLASS_TEXT
                        binding.tilBuscarCliente.hint = "DNI / Cliente"
                        binding.tilRazonSocial.visibility = View.GONE
                    }

                    binding.btnFactura.id -> {
                        idTipoComprobanteSeleccionado = 2
                        binding.actvBuscarCliente.inputType =
                            android.text.InputType.TYPE_CLASS_NUMBER
                        binding.tilBuscarCliente.hint = "RUC (11 dígitos)"
                        binding.tilRazonSocial.visibility = View.VISIBLE
                    }

                    binding.btnTicket.id -> {
                        idTipoComprobanteSeleccionado = 3
                        binding.actvBuscarCliente.inputType =
                            android.text.InputType.TYPE_CLASS_TEXT
                        binding.tilBuscarCliente.hint = "Cliente (Opcional)"
                        binding.tilRazonSocial.visibility = View.GONE
                    }
                }
            }
        }

        // 3. Lógica de Montos y Vuelto (Se mantiene y mejora)
        binding.tvTotalDialogo.text =
            String.format(Locale.US, "S/ %.2f", total)

        fun validarPago() {
            val montoTexto = binding.etMontoRecibido.text.toString()
            val recibido = montoTexto.toDoubleOrNull() ?: 0.0
            val esTarjeta =
                binding.toggleMetodoPago.checkedButtonId == binding.btnTarjeta.id
            val vuelto = recibido - total

            if (esTarjeta) {
                binding.tvVuelto.text = "Pago con Tarjeta"
                binding.tvVuelto.setTextColor(Color.GRAY)
                binding.btnConfirmarPago.isEnabled = true
            } else {
                if (vuelto >= 0) {
                    binding.tvVuelto.text =
                        String.format(Locale.US, "Vuelto: S/ %.2f", vuelto)
                    binding.tvVuelto.setTextColor(Color.parseColor("#4CAF50"))
                    binding.btnConfirmarPago.isEnabled = true
                } else {
                    binding.tvVuelto.text =
                        String.format(Locale.US, "Falta: S/ %.2f", Math.abs(vuelto))
                    binding.tvVuelto.setTextColor(Color.RED)
                    binding.btnConfirmarPago.isEnabled = false
                }
            }
        }

        // Listeners de montos rápidos
        fun setMontoRapido(monto: Double) {
            binding.etMontoRecibido.setText(String.format(Locale.US, "%.2f", monto))
            binding.etMontoRecibido.setSelection(
                binding.etMontoRecibido.text?.length ?: 0
            )
            validarPago()
        }

        binding.btnExacto.setOnClickListener { setMontoRapido(total) }
        binding.btnMonto10.setOnClickListener { setMontoRapido(10.0) }
        binding.btnMonto20.setOnClickListener { setMontoRapido(20.0) }
        binding.btnMonto50.setOnClickListener { setMontoRapido(50.0) }
        binding.btnMonto100.setOnClickListener { setMontoRapido(100.0) }

        binding.etMontoRecibido.addTextChangedListener { validarPago() }
        binding.toggleMetodoPago.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val esTarjeta = checkedId == binding.btnTarjeta.id
                binding.containerEfectivo.visibility =
                    if (esTarjeta) View.GONE else View.VISIBLE
                validarPago()
            }
        }

        // 4. Botón Finalizar con Validaciones de Factura
        binding.btnConfirmarPago.setOnClickListener {
            val documento = binding.actvBuscarCliente.text.toString()
            val razonSocial = binding.etRazonSocial.text.toString()

            // Validaciones para Factura
            if (idTipoComprobanteSeleccionado == 2) {
                if (documento.length != 11) {
                    binding.tilBuscarCliente.error = "El RUC debe tener 11 dígitos"
                    return@setOnClickListener
                }
                if (razonSocial.isEmpty()) {
                    binding.tilRazonSocial.error = "Ingrese Razón Social"
                    return@setOnClickListener
                }
            }

            val metodo =
                if (binding.toggleMetodoPago.checkedButtonId == binding.btnTarjeta.id)
                    "TARJETA" else "EFECTIVO"

            // Llamada final al ViewModel
            viewModel.saveSale(
                idCliente = clienteDelDialogo?.id,
                idEmpleado = viewModel.userId,
                metodoPago = metodo,
                idTipoComprobante = idTipoComprobanteSeleccionado
            )

            dialog.dismiss()
        }

        binding.btnCancelarPago.setOnClickListener {
            DialogHelper.showConfirmation(
                context = this,
                title = "Cancelar Venta",
                message = "¿Estás seguro de que deseas cancelar la venta?",
                onConfirm = {
                    ToastHelper.clasicCustomToast(binding.root, "Venta cancelada", true)
                    dialog.dismiss() })
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