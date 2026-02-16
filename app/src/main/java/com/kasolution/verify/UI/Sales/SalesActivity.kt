package com.kasolution.verify.UI.Sales

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.UI.Sales.adapter.SearchClienteAdapter
import com.kasolution.verify.UI.Sales.adapter.SearchProductAdapter
import com.kasolution.verify.UI.Sales.adapter.VentaAdapter
import com.kasolution.verify.UI.Sales.model.CartItem
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivitySalesBinding
import com.kasolution.verify.databinding.DialogPagoBinding
import java.util.Locale

class SalesActivity : AppCompatActivity() {
    private val TAG = "SalesActivity"
    private lateinit var binding: ActivitySalesBinding

    private val listaCarrito = mutableListOf<CartItem>()
    private var listaMaestra = listOf<Product>()
    private var listaClientes= listOf<Cliente>()

    private lateinit var adapterVenta: VentaAdapter

    private val viewModel: SalesViewModel by viewModels {
        AppProvider.provideSalesViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialRecycler()
        setupObservers()

        viewModel.loadProducts()
        viewModel.loadClientes()

        binding.btnCobrar.setOnClickListener {
            if (listaCarrito.isNotEmpty()) {
                val total = listaCarrito.sumOf { it.cantidad * it.producto.precioVenta }
                mostrarDialogoPago(total)
            }
        }
    }

    private fun initialRecycler() {
        adapterVenta = VentaAdapter(
            cartList = listaCarrito,
            onIncrementClick = { item, position -> incrementarItem(item, position) },
            onDecrementClick = { item, position -> decrementarItem(item, position) },
            onDeleteClick = { position -> confirmarEliminacion(position) }
        )

        binding.rvCarrito.apply {
            layoutManager = LinearLayoutManager(this@SalesActivity)
            adapter = adapterVenta
        }
    }

    private fun setupObservers() {
        viewModel.productsList.observe(this) { lista ->
            listaMaestra = lista
            setupBuscadorManual(lista)
        }
        viewModel.clientsList.observe(this) { lista ->
            listaClientes = lista
        }

        viewModel.exception.observe(this) { error ->
            ToastHelper.showCustomToast(binding.root, error, false)
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading) ProgressHelper.showProgress(this, "Cargando productos...")
            else ProgressHelper.hideProgress()
        }
    }

    private fun setupBuscadorManual(listaProductos: List<Product>) {
        val searchAdapter = SearchProductAdapter(this, listaProductos)

        binding.etBuscarVenta.apply {
            setAdapter(searchAdapter)
            threshold = 1

            // --- NUEVO: Configuración del botón Check del teclado ---
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.root.hideKeyboard() // Usamos tu extension function
                    clearFocus()
                    true
                } else false
            }

            setOnItemClickListener { parent, _, position, _ ->
                val productoSeleccionado = parent.getItemAtPosition(position) as Product
                agregarAlCarrito(productoSeleccionado)

                post {
                    setText("", false)
                    requestFocus()
                }
            }
        }
    }

    private fun agregarAlCarrito(producto: Product) {
        if (producto.stock <= 0) {
            ToastHelper.showCustomToast(binding.root, "Sin stock disponible", false)
            return
        }

        val indexExistente = listaCarrito.indexOfFirst { it.producto.id == producto.id }

        if (indexExistente != -1) {
            incrementarItem(listaCarrito[indexExistente], indexExistente)
        } else {
            val nuevoItem = CartItem(producto = producto, cantidad = 1)
            listaCarrito.add(0, nuevoItem)
            adapterVenta.notifyItemInserted(0)
            binding.rvCarrito.scrollToPosition(0)
            actualizarTotalUI()
        }
    }

    private fun incrementarItem(item: CartItem, position: Int) {
        val stockReal = listaMaestra.find { it.id == item.producto.id }?.stock ?: 0

        if (item.cantidad < stockReal) {
            item.cantidad++
            val actualIndex = listaCarrito.indexOf(item)
            if (actualIndex != -1) {
                adapterVenta.notifyItemChanged(actualIndex)
            }
            actualizarTotalUI()
        } else {
            ToastHelper.showCustomToast(binding.root, "Stock máximo alcanzado", false)
        }
    }

    private fun decrementarItem(item: CartItem, position: Int) {
        if (item.cantidad > 1) {
            item.cantidad--
            val actualIndex = listaCarrito.indexOf(item)
            if (actualIndex != -1) {
                adapterVenta.notifyItemChanged(actualIndex)
            }
            actualizarTotalUI()
        } else {
            val actualIndex = listaCarrito.indexOf(item)
            confirmarEliminacion(if (actualIndex != -1) actualIndex else position)
        }
    }

    private fun confirmarEliminacion(position: Int) {
        if (position < 0 || position >= listaCarrito.size) return
        val item = listaCarrito[position]
        AlertDialog.Builder(this)
            .setTitle("Quitar producto")
            .setMessage("¿Deseas quitar ${item.producto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarItem(position) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarItem(position: Int) {
        listaCarrito.removeAt(position)
        adapterVenta.notifyItemRemoved(position)
        adapterVenta.notifyItemRangeChanged(position, listaCarrito.size)
        actualizarTotalUI()
    }

    private fun actualizarTotalUI() {
        val total = listaCarrito.sumOf { it.cantidad * it.producto.precioVenta }
        binding.tvTotalPagar.text = String.format(Locale.US, "S/ %.2f", total)
        binding.btnCobrar.isEnabled = listaCarrito.isNotEmpty()
        binding.btnCobrar.alpha = if (listaCarrito.isEmpty()) 0.5f else 1.0f
    }

    private fun mostrarDialogoPago(total: Double) {
        val dialogBinding = DialogPagoBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        builder.setCancelable(false) // Para obligar a terminar o cancelar con botón
        val clienteAdapter = SearchClienteAdapter(this, listaClientes)
        dialogBinding.actvBuscarCliente.setAdapter(clienteAdapter)

        dialogBinding.tvTotalDialogo.text = "Total: S/ ${String.format("%.2f", total)}"

        builder.setPositiveButton("FINALIZAR", null) // Ponemos null para configurar el clic después
        builder.setNegativeButton("CANCELAR", null)

        val alertDialog = builder.create()

        // Mostramos el diálogo primero para poder manipular sus botones
        alertDialog.show()

        val btnFinalizar = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Listener para el monto recibido
        dialogBinding.etMontoRecibido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val montoTexto = s.toString()
                val recibido = montoTexto.toDoubleOrNull() ?: 0.0
                val vuelto = recibido - total

                // Lógica de Vuelto
                if (vuelto >= 0) {
                    dialogBinding.tvVuelto.text = "Vuelto: S/ ${String.format("%.2f", vuelto)}"
                    dialogBinding.tvVuelto.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    dialogBinding.tvVuelto.text = "Falta: S/ ${String.format("%.2f", Math.abs(vuelto))}"
                    dialogBinding.tvVuelto.setTextColor(android.graphics.Color.RED)
                }

                // Validación del botón Finalizar:
                // Se habilita si es TARJETA (no importa monto) o si es EFECTIVO y pagó completo
                val esTarjeta = dialogBinding.toggleMetodoPago.checkedButtonId == dialogBinding.btnTarjeta.id
                btnFinalizar.isEnabled = esTarjeta || recibido >= total
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener para el cambio de método (Efectivo/Tarjeta)
        dialogBinding.toggleMetodoPago.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val recibido = dialogBinding.etMontoRecibido.text.toString().toDoubleOrNull() ?: 0.0
                val esTarjeta = checkedId == dialogBinding.btnTarjeta.id

                // Si es tarjeta, deshabilitamos el campo de monto recibido para evitar confusión
                dialogBinding.etMontoRecibido.isEnabled = !esTarjeta
                if (esTarjeta) dialogBinding.etMontoRecibido.setText(total.toString())

                btnFinalizar.isEnabled = esTarjeta || recibido >= total
            }
        }

        // Configuramos el clic real del botón finalizar
        btnFinalizar.setOnClickListener {
            val metodo = if (dialogBinding.toggleMetodoPago.checkedButtonId == dialogBinding.btnTarjeta.id)
                "TARJETA" else "EFECTIVO"

            procesarVentaFinal(total, metodo)
            alertDialog.dismiss()
        }
    }

    private fun procesarVentaFinal(total: Double, metodo: String) {
        // Aquí es donde llamas al ViewModel para guardar en la base de datos
        // Enviamos listaCarrito, total y metodo de pago
        ToastHelper.showCustomToast(binding.root, "Venta guardada con éxito", true)

        // Limpiamos todo para la siguiente venta
        listaCarrito.clear()
        adapterVenta.notifyDataSetChanged()
        actualizarTotalUI()
    }


    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}