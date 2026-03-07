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
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
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
import com.kasolution.verify.domain.sales.model.CartItem
import java.util.Locale

class SalesActivity : AppCompatActivity() {
    private val TAG = "SalesActivity"
    private lateinit var binding: ActivitySalesBinding
    private lateinit var adapterVenta: CartAdapter

    private var listaMaestra = listOf<Product>()
    private var listaClientes = listOf<Client>()
    private var clienteSeleccionado: Client? = null
    private var idTipoComprobanteSeleccionado = 1

    private val viewModel: SalesViewModel by viewModels {
        AppProvider.provideSalesViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupBackPressedHandling()
        initialRecycler()
        setupObservers()

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

                    ToastHelper.clasicCustomToast(binding.root, "Venta cancelada", true)
                }
            )
        }
        binding.tilBuscarVenta.isEndIconVisible = false

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
            onDeleteClick = { position ->
                val item = viewModel.cartList.value?.getOrNull(position)
                item?.let { confirmarEliminacion(it) }
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
            // Si el carrito está vacío, podrías mostrar un texto de "Carrito vacío"
            binding.tvTotalPagar.visibility = if (nuevaLista.isEmpty()) View.GONE else View.VISIBLE

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
        AlertDialog.Builder(this)
            .setTitle("Quitar producto")
            .setMessage("¿Deseas quitar ${item.producto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.removeItem(item.producto.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarTotalUI(lista: List<CartItem>) {
        val total = lista.sumOf { it.subtotal }
        binding.tvTotalPagar.text = String.Companion.format(Locale.US, "S/ %.2f", total)
        binding.btnCobrar.isEnabled = lista.isNotEmpty()
        binding.btnCobrar.alpha = if (lista.isEmpty()) 0.5f else 1.0f
    }

    private fun mostrarDialogoPago(total: Double) {
        val dialogBinding = DialogPagoBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false)

        val alertDialog = builder.create()
        var clienteDelDialogo: Client? = null
        idTipoComprobanteSeleccionado = 1 // Reset a Boleta al abrir

        // 1. Configurar Buscador de Clientes
        val clienteAdapter = SearchClienteAdapter(this, listaClientes)
        dialogBinding.actvBuscarCliente.apply {
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
                    dialogBinding.etRazonSocial.setText(cliente.nombre)

                    // 3. Limpiamos errores visuales si existían
                    dialogBinding.tilBuscarCliente.error = null
                    dialogBinding.tilRazonSocial.error = null
                } else {
                    // Para Boleta o Ticket, también mantenemos el número en el campo
                    setText(cliente.dniRuc, false)
                }
                hideKeyboard()
                dialogBinding.tilBuscarCliente.error = null
            }
        }

        // 2. Lógica de Selección de Comprobante (NUEVO)
        dialogBinding.toggleTipoComprobante.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    dialogBinding.btnBoleta.id -> {
                        idTipoComprobanteSeleccionado = 1
                        dialogBinding.actvBuscarCliente.inputType = android.text.InputType.TYPE_CLASS_TEXT
                        dialogBinding.tilBuscarCliente.hint = "DNI / Cliente"
                        dialogBinding.tilRazonSocial.visibility = View.GONE
                    }
                    dialogBinding.btnFactura.id -> {
                        idTipoComprobanteSeleccionado = 2
                        dialogBinding.actvBuscarCliente.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        dialogBinding.tilBuscarCliente.hint = "RUC (11 dígitos)"
                        dialogBinding.tilRazonSocial.visibility = View.VISIBLE
                    }
                    dialogBinding.btnTicket.id -> {
                        idTipoComprobanteSeleccionado = 3
                        dialogBinding.actvBuscarCliente.inputType = android.text.InputType.TYPE_CLASS_TEXT
                        dialogBinding.tilBuscarCliente.hint = "Cliente (Opcional)"
                        dialogBinding.tilRazonSocial.visibility = View.GONE
                    }
                }
            }
        }

        // 3. Lógica de Montos y Vuelto (Se mantiene y mejora)
        dialogBinding.tvTotalDialogo.text = String.format(Locale.US, "Total a pagar: S/ %.2f", total)

        fun validarPago() {
            val montoTexto = dialogBinding.etMontoRecibido.text.toString()
            val recibido = montoTexto.toDoubleOrNull() ?: 0.0
            val esTarjeta = dialogBinding.toggleMetodoPago.checkedButtonId == dialogBinding.btnTarjeta.id
            val vuelto = recibido - total

            if (esTarjeta) {
                dialogBinding.tvVuelto.text = "Pago con Tarjeta"
                dialogBinding.tvVuelto.setTextColor(Color.GRAY)
                dialogBinding.btnConfirmarPago.isEnabled = true
            } else {
                if (vuelto >= 0) {
                    dialogBinding.tvVuelto.text = String.format(Locale.US, "Vuelto: S/ %.2f", vuelto)
                    dialogBinding.tvVuelto.setTextColor(Color.parseColor("#4CAF50"))
                    dialogBinding.btnConfirmarPago.isEnabled = true
                } else {
                    dialogBinding.tvVuelto.text = String.format(Locale.US, "Falta: S/ %.2f", Math.abs(vuelto))
                    dialogBinding.tvVuelto.setTextColor(Color.RED)
                    dialogBinding.btnConfirmarPago.isEnabled = false
                }
            }
        }

        // Listeners de montos rápidos
        fun setMontoRapido(monto: Double) {
            dialogBinding.etMontoRecibido.setText(String.format(Locale.US, "%.2f", monto))
            dialogBinding.etMontoRecibido.setSelection(dialogBinding.etMontoRecibido.text?.length ?: 0)
            validarPago()
        }

        dialogBinding.btnExacto.setOnClickListener { setMontoRapido(total) }
        dialogBinding.btnMonto10.setOnClickListener { setMontoRapido(10.0) }
        dialogBinding.btnMonto20.setOnClickListener { setMontoRapido(20.0) }
        dialogBinding.btnMonto50.setOnClickListener { setMontoRapido(50.0) }
        dialogBinding.btnMonto100.setOnClickListener { setMontoRapido(100.0) }

        dialogBinding.etMontoRecibido.addTextChangedListener { validarPago() }
        dialogBinding.toggleMetodoPago.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val esTarjeta = checkedId == dialogBinding.btnTarjeta.id
                dialogBinding.containerEfectivo.visibility = if (esTarjeta) View.GONE else View.VISIBLE
                validarPago()
            }
        }

        // 4. Botón Finalizar con Validaciones de Factura
        dialogBinding.btnConfirmarPago.setOnClickListener {
            val documento = dialogBinding.actvBuscarCliente.text.toString()
            val razonSocial = dialogBinding.etRazonSocial.text.toString()

            // Validaciones para Factura
            if (idTipoComprobanteSeleccionado == 2) {
                if (documento.length != 11) {
                    dialogBinding.tilBuscarCliente.error = "El RUC debe tener 11 dígitos"
                    return@setOnClickListener
                }
                if (razonSocial.isEmpty()) {
                    dialogBinding.tilRazonSocial.error = "Ingrese Razón Social"
                    return@setOnClickListener
                }
            }

            val metodo = if (dialogBinding.toggleMetodoPago.checkedButtonId == dialogBinding.btnTarjeta.id)
                "TARJETA" else "EFECTIVO"

            // Llamada final al ViewModel
            viewModel.saveSale(
                idCliente = clienteDelDialogo?.id,
                idEmpleado = 1, // Cambiar por ID de sesión real
                metodoPago = metodo,
                idTipoComprobante = idTipoComprobanteSeleccionado
            )

            alertDialog.dismiss()
        }

        dialogBinding.btnCancelarPago.setOnClickListener { alertDialog.dismiss() }

        alertDialog.show()
    }

    private val scannerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action){
                "ACTION_PRODUCT_SCANNED"->{
                    val codigoSaneado = intent.getStringExtra("SCAN_RESULT_CODE") ?: return
                    // Buscamos en el LiveData del VM directamente
                    val producto = viewModel.productsList.value?.find { it.codigo == codigoSaneado }

                    if (producto != null) {
                        // USAR postValue o runOnUiThread es vital aquí
                        viewModel.addProductToCart(producto)
                        Log.d("SCANNER_DEBUG", "Producto encontrado y enviado al VM: ${producto.nombre}")
                    } else {
                        Log.e("SCANNER_DEBUG", "Código $codigoSaneado no existe en la lista del VM")
                    }
                }
                "ACTION_CLEAR_CART"-> {
                    Log.d("SCANNER_DEBUG", "Recibido ACTION_CLEAR_CART")
                    viewModel.clearCart()
                    ToastHelper.clasicCustomToast(binding.root,"Carrito vacio",true)
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