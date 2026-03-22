package com.kasolution.verify.UI.Purchase.currentPurchase

import android.R.attr.text
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Purchase.adapter.PurchaseAdapter
import com.kasolution.verify.UI.Purchase.adapter.SearchSupplierAdapter
import com.kasolution.verify.databinding.ActivityPurchaseBinding
import com.kasolution.verify.UI.Purchase.model.PurchaseItem
import com.kasolution.verify.UI.Purchase.viewModel.PurchaseViewModel
import com.kasolution.verify.UI.Sales.adapter.SearchProductAdapter
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.DialogChangePriceBinding
import com.kasolution.verify.databinding.DialogChangeQuantityBinding
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.supplier.model.Supplier
import java.util.Locale
import kotlin.getValue

class PurchaseActivity : AppCompatActivity() {
    private val TAG = "PurchaseActivity"
    private lateinit var binding: ActivityPurchaseBinding
    private lateinit var adapterCompra: PurchaseAdapter

    private var supplierSeleccionado: Supplier? = null
    private val viewModel: PurchaseViewModel by viewModels {
        AppProvider.providePurchaseViewModelFactory(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupScannerReceiver()
        setupBackPressedHandling()
        initialRecycler()
        setupObservers()
        setupListeners()
    }
    private fun setupListeners() {
        // Botón para finalizar la compra (Nota de Ingreso)
        binding.btnFinalizarCompra.setOnClickListener {
            val total = viewModel.totalCompra.value ?: 0.0
            if (supplierSeleccionado == null) {
                ToastHelper.showCustomToast(binding.root, "Seleccione un proveedor", false)
                return@setOnClickListener
            }
            if (total <= 0) {
                ToastHelper.showCustomToast(binding.root, "El carrito está vacío", false)
                return@setOnClickListener
            }

            DialogHelper.showConfirmation(
                context = this,
                title = "Registrar Compra",
                message = "¿Confirmas el ingreso de mercadería por S/ ${String.format("%.2f", total)}?",
                onConfirm = {
                    val idEmpleado = viewModel.userId
                    viewModel.savePurchase(supplierSeleccionado!!.id, idEmpleado)
                }
            )
        }

        // Limpiar carrito
        binding.tilBuscarCompra.setEndIconOnClickListener {
            DialogHelper.showConfirmation(
                context = this,
                title = "¿Vaciar Carrito?",
                message = "Se eliminarán todos los productos de la compra actual.",
                onConfirm = { viewModel.clearCart() }
            )
        }
        binding.btnScannerCompra.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java).apply {
                putExtra("SCAN_MODE", "PURCHASE")
                // Activamos el modo multi-escaneo para no salir de la cámara tras cada producto
                putExtra("MULTI_SCAN", true)
                // Pasamos datos actuales para que el escáner muestre el progreso visual
                putExtra("INITIAL_TOTAL", viewModel.totalCompra.value ?: 0.0)
                putExtra("INITIAL_COUNT", viewModel.cartList.value?.sumOf { it.cantidad } ?: 0)
            }
            startActivity(intent)
        }
    }

    private fun initialRecycler() {
        adapterCompra = PurchaseAdapter(
            purchaseList = mutableListOf(),
            onIncrementClick = { item, _ ->
                viewModel.updateQuantity(item.idProducto, item.cantidad + 1)
            },
            onDecrementClick = { item, _ ->
                if (item.cantidad > 1) {
                    viewModel.updateQuantity(item.idProducto, item.cantidad - 1)
                } else {
                    confirmarEliminacion(item)
                }
            },
            onPriceEditClick = { item, _ ->
                showPriceDialog(item)
            },
            onDeleteClick = { position ->
                // Implementar lógica de eliminación por posición
                val item = viewModel.cartList.value?.getOrNull(position)
                item?.let { confirmarEliminacion(it) }
            },
            onQuantityClick = { idProducto, cantidad ->
                showQuantityDialog(idProducto, cantidad)
            }
        )

        binding.rvPurchase.apply { // Asegúrate que el ID coincida con tu XML
            layoutManager = LinearLayoutManager(this@PurchaseActivity)
            adapter = adapterCompra
        }
    }

    private fun showPriceDialog(item: PurchaseItem) {
        DialogHelper.showPriceDialog(
            context = this,
            productName = item.nombre,
            currentPrice = item.precioCompra,
            colorRes = R.color.green_pos // Tu color de acento para Compras
        ) { nuevoPrecio ->
            // Este bloque (lambda) se ejecuta solo cuando el usuario da a "ACTUALIZAR"
            viewModel.updatePrice(item.idProducto, nuevoPrecio)
        }
    }

    private fun showQuantityDialog(item: PurchaseItem, position: Int) {
        DialogHelper.showQuantityDialog(
            context = this,
            productName = item.nombre,
            currentQuantity = item.cantidad,
            colorRes = R.color.green_pos
        ) { nuevaCantidad ->
            viewModel.updateQuantity(item.idProducto, nuevaCantidad)
        }
    }

    private fun setupObservers() {
        // Observador del Carrito
        viewModel.cartList.observe(this) { nuevaLista ->
            adapterCompra.updateList(nuevaLista)
            binding.tilBuscarCompra.isEndIconVisible = nuevaLista.isNotEmpty()
            binding.btnFinalizarCompra.isEnabled = nuevaLista.isNotEmpty()
            binding.btnFinalizarCompra.alpha = if (nuevaLista.isEmpty()) 0.5f else 1.0f
        }

        // Observador del Total
        viewModel.totalCompra.observe(this) { total ->
            binding.tvTotalCompra.text = String.format(Locale.US, "S/ %.2f", total)
        }

        // Observador de Productos (para el buscador)
        viewModel.productsList.observe(this) { lista ->
            val searchAdapter = SearchProductAdapter(this, lista)
            binding.etBuscarCompra.apply {
                setAdapter(searchAdapter)
                setOnItemClickListener { parent, _, position, _ ->
                    val producto = parent.getItemAtPosition(position) as Product
                    viewModel.addProductToCart(producto)
                    setText("", false)
                }
            }
        }

        // Observador de Proveedores
        viewModel.suppliersList.observe(this) { lista ->
            val supplierAdapter = SearchSupplierAdapter(this, lista)
            binding.etBuscarProveedor.apply {
                setAdapter(supplierAdapter)
                setOnItemClickListener { parent, _, position, _ ->
                    supplierSeleccionado = parent.getItemAtPosition(position) as Supplier
                    hideKeyboard()
                }
            }
        }

        // Estado de la operación
        viewModel.operationSuccess.observe(this) { accion ->
            if (accion == "PURCHASE_SAVE") {
                viewModel.resetOperationStatus()
                supplierSeleccionado = null
                binding.etBuscarProveedor.setText("")
                ToastHelper.showCustomToast(binding.root, "Ingreso de mercadería registrado", true)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading) ProgressHelper.showProgress(this, "Procesando...")
            else ProgressHelper.hideProgress()
        }

        viewModel.exception.observe(this) { error ->
            ToastHelper.showCustomToast(binding.root, error, false)
        }
    }

    private fun confirmarEliminacion(item: PurchaseItem) {
        DialogHelper.showConfirmation(
            context = this,
            title = "Eliminar Producto",
            message = "¿Deseas eliminar ${item.nombre} de la lista de compra?",
            onConfirm = {
                val pos = viewModel.cartList.value?.indexOfFirst { it.idProducto == item.idProducto } ?: -1
                if (pos != -1) viewModel.removeItem(pos)
            })
    }

    // --- ESCÁNER Y NAVEGACIÓN ---

    private fun setupScannerReceiver() {
        val filter = IntentFilter("ACTION_PRODUCT_SCANNED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scannerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scannerReceiver, filter)
        }
    }

    private val scannerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_PRODUCT_SCANNED" -> {
                    val codigoSaneado = intent.getStringExtra("SCAN_RESULT_CODE") ?: return

                    // Buscamos el producto en la lista que el ViewModel ya cargó
                    val producto = viewModel.productsList.value?.find { it.codigo == codigoSaneado }

                    if (producto != null) {
                        // Agregamos al carrito de compras (esto disparará el LiveData)
                        viewModel.addProductToCart(producto)
                        Log.d(TAG, "Producto escaneado y agregado: ${producto.nombre}")
                    } else {
                        ToastHelper.showCustomToast(binding.root, "Código no registrado: $codigoSaneado", false)
                    }
                }
                "ACTION_CLEAR_CART" -> {
                    viewModel.clearCart()
                    ToastHelper.showCustomToast(binding.root, "Lista de compra vaciada", true)
                }
            }
        }
    }

    private fun setupBackPressedHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.cartList.value.isNullOrEmpty()) {
                    DialogHelper.showConfirmation(
                        this@PurchaseActivity,
                        "Cancelar Compra",
                        "Hay productos en la lista. ¿Deseas salir?",
                        onConfirm = { finish() }
                    )
                } else {
                    finish()
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
        unregisterReceiver(scannerReceiver)
    }

}