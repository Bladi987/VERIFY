package com.kasolution.verify.UI.Sales.CurrentSale

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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Sales.adapter.CartAdapter
import com.kasolution.verify.UI.Sales.adapter.SearchClienteAdapter
import com.kasolution.verify.UI.Sales.adapter.SearchProductAdapter
import com.kasolution.verify.UI.Sales.fragment.PagoDialogFragment
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
import com.kasolution.verify.core.utils.setupCurrencyFormatting
import com.kasolution.verify.databinding.LayoutOtrosPagosBinding
import java.util.Locale

class SalesActivity : AppCompatActivity() {
    private val TAG = "SalesActivity"
    private lateinit var binding: ActivitySalesBinding
    private lateinit var adapterVenta: CartAdapter

    private var listaMaestra = listOf<Product>()
    private var listaClientes = listOf<Client>()
    private var idSucursal=0

    private val viewModel: SalesViewModel by viewModels {
        AppProvider.provideSalesViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(viewModel.activeCashSessionId>0){
            setupPreferences()
            setupScannerReceiver()
            setupBackPressedHandling()
            initialRecycler()
            setupObservers()
            setupListeners()
        }else{
            finish()
        }
    }

    private fun setupPreferences() {
        idSucursal=viewModel.idSucursal
    }

    private fun setupListeners() {
        binding.btnCobrar.setOnClickListener {
            // El total ahora viene del LiveData del ViewModel
            val total = viewModel.totalVenta.value ?: 0.0
            if (total > 0) {
//                mostrarDialogoPago(total)

                val dialogo = PagoDialogFragment.newInstance(total, listaClientes) { idCliente, idComprobante, listaPagos ->
                    // Toda la lógica de guardado ocurre aquí al recibir el callback
                    viewModel.saveSale(
                        idCliente = idCliente,
                        idEmpleado = viewModel.userId,
                        pagos = listaPagos,
                        idTipoComprobante = idComprobante
                    )
                }
                dialogo.show(supportFragmentManager, "PagoDialogFragment")
            }
        }
        binding.btnScannerVenta.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java).apply {
                putExtra("MULTI_SCAN", true)
                putExtra("SCAN_MODE", "SALE")
                putExtra("ID_SUCURSAL", idSucursal)
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