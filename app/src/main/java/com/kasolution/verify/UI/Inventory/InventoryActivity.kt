package com.kasolution.verify.UI.Inventory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.kasolution.verify.R
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Inventory.adapter.InventoryAdapter
import com.kasolution.verify.UI.Inventory.fragment.ProductFormDialogFragment
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.UI.Inventory.viewModel.InventoryViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.BottomSheetHelper
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityInventoryBinding
import com.kasolution.verify.databinding.ItemCardInventoryShowBinding
import kotlin.getValue

class InventoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInventoryBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: InventoryAdapter
    private lateinit var lista: ArrayList<Product>
    private var selectedProduct: Product? = null
    private val TAG = "InventoryActivity"

    private val viewModel: InventoryViewModel by viewModels {
        AppProvider.provideInventoryViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        lista = ArrayList()
        initRecycler()
        setupObservers()

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ejecutar el filtro cada vez que el texto cambie
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) {
                    // Buscamos el primer chip (que es "Todos") y lo marcamos
                    val firstChip = binding.chipGroupCategories.getChildAt(0) as? Chip

                    // Solo lo marcamos si no estaba ya marcado para evitar llamadas innecesarias al filtro
                    if (firstChip != null && !firstChip.isChecked) {
                        firstChip.isChecked = true
                    }
                }
                adapter.filter.filter(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        binding.fabAddProducto.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = ProductFormDialogFragment()
            // El "InventoryTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "ProductTag")
        }

        binding.tilCodigoProducto.setEndIconOnClickListener {
//            barcodeLauncher.launch(Intent(this, ScannerActivity::class.java))
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("SINGLE_SCAN", true) // ACTIVAR MODO MULTI
            scannerLauncher.launch(intent)
        }

        binding.btnRetry.setOnClickListener {
            binding.etBuscar.setText("")
            // Opcional: Marcar chip "Todos"
            (binding.chipGroupCategories.getChildAt(0) as? Chip)?.isChecked = true
            viewModel.loadProducts()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (BottomSheetHelper.isSheetVisible()) {
                    // Si el menú está abierto, lo cerramos y NO salimos de la activity
                    BottomSheetHelper.closeSheetDirectly()
                } else {
                    // Si el menú NO está abierto, desactivamos este callback y dejamos que
                    // la activity se cierre normalmente con el siguiente "atrás"
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }

        })

    }

    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = InventoryAdapter(
            listaInicial = lista,
            onClickListener = { product -> onItemClicListener(product) },
            onLongClickListener = { product, position -> showOptionsFor(product, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvProducts.layoutManager = lmanager
        binding.rvProducts.adapter = adapter
    }
    private fun setupObservers() {
        // Lista de Productos
        viewModel.productsList.observe(this) { lista ->
            mostrarMensajeOrLista(exito = true, lista = lista.isNullOrEmpty())
            adapter.updateList(lista ?: emptyList())
        }

        // Categorías para Chips
        viewModel.categoryList.observe(this) { lista ->
            setupCategoryChips(lista)
        }

        // Errores
        viewModel.exception.observe(this) { error ->
            viewModel.resetOperationStatus()
            if (error.isNotEmpty()) {
                val sufijo = error.split(":")[1].trim()
                val errorProceso =
                    if (sufijo == "PRODUCT_SAVE" || sufijo == "PRODUCT_UPDATE" || sufijo == "PRODUCT_DELETE" || sufijo == "CATEGORY_SAVE") true else false
                if (errorProceso)
                    ToastHelper.clasicCustomToast(binding.root, "Error al procesar $sufijo", false)
                else
                    mostrarMensajeOrLista(exito = false, mensaje = error)
            }
        }

        // Éxito de Operación (Consistente con Clientes)
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "PRODUCT_SAVE" -> "¡Producto registrado!"
                "PRODUCT_UPDATE" -> "¡Producto actualizado!"
                "PRODUCT_DELETE" -> "Producto eliminado"
                "CATEGORY_SAVE" -> "Categoría creada"
                else -> ""
            }
            if (mensaje.isNotEmpty()) ToastHelper.clasicCustomToast(binding.root, mensaje, true)

            // Resetear estado después de un breve delay
            binding.root.postDelayed({ viewModel.resetOperationStatus() }, 100)
        }

        // Loading Progress (Consistente con Clientes)
        viewModel.isLoading.observe(this) { loading ->
            if (loading) ProgressHelper.showProgress(this, "Sincronizando...")
            else ProgressHelper.hideProgress()
        }
    }

    private fun onItemClicListener(product: Product) {
       dialogShowProduct(product)
    }

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigoEscaneado = result.data?.getStringExtra("SCAN_RESULT")
                binding.etBuscar.setText(codigoEscaneado) // Ponemos el código en el campo
            }
        }

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val codigoEscaneado = result.data?.getStringExtra("SCAN_RESULT")
            binding.etBuscar.setText(codigoEscaneado)
        }
    }
    private fun showOptionsFor(product: Product, position: Int) {
        // Cerramos el teclado si está abierto para que no estorbe al menú
        binding.etBuscar.clearFocus()

        selectedProduct = product
        adapter.setSelectedItem(position)

        BottomSheetHelper.showInventoryOptions(
            activity = this,
            cabeceraName = "Producto",
            name = product.nombre,
            onEdit = {
                // Reutilizamos la lógica que ya tenías
                val dialog = ProductFormDialogFragment.newInstance(product)
                dialog.show(supportFragmentManager, "EditProduct")
            },
            onDelete = {
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Producto",
                    "¿Estás seguro de que deseas eliminar a ${product.nombre}?",
                    onConfirm = {
                        viewModel.deleteProduct(product.id)
                    })
            },
            onDismiss = {
                // Limpieza visual cuando el menú se va
                selectedProduct = null
                adapter.clearSelection()
            }
        )
    }
    fun mostrarMensajeOrLista(
        exito: Boolean,
        lista: Boolean = false,
        mensaje: String = "Sin datos que mostrar",
        imagenRes: Int = R.drawable.no_signal
    ) {
        // Actualizamos la imagen SIEMPRE antes de mostrar el layout
        binding.ivImageMessage.setImageResource(imagenRes)
        binding.fabAddProducto.isEnabled = exito
//        binding.etBuscar.isEnabled = exito
        binding.tilCodigoProducto.isEnabled = exito
        binding.fabAddProducto.alpha = if (exito) 1.0f else 0.5f
        if (exito) {
            if (lista) {
                // Éxito pero sin datos (Filtros o Inventario vacío)
                binding.rvProducts.isVisible = false
                binding.layoutMessage.isVisible = true
                binding.tvTitleMeassage.text = mensaje
                binding.btnRetry.isVisible = false
                binding.fabAddProducto.isEnabled = true
            } else {
                // Éxito y con datos
                binding.rvProducts.isVisible = true
                binding.layoutMessage.isVisible = false
                binding.fabAddProducto.isEnabled = true
            }
        } else {
            // Error de conexión (Sin éxito)
            binding.rvProducts.isVisible = false
            binding.layoutMessage.isVisible = true
            binding.tvTitleMeassage.text = mensaje
            binding.btnRetry.isVisible = true
            binding.fabAddProducto.isEnabled = false
        }
    }

    private fun dialogShowProduct(product: Product, position: Int = -1) {
        val binding = ItemCardInventoryShowBinding.inflate(layoutInflater)
        binding.apply {
            tvCodigo.text = product.codigo
            tvNombre.text = product.nombre
            tvCategoria.text = product.nombreCategoria
            tvProveedor.text = product.nombreProveedor
            tvPcompra.text = "S/ ${String.format("%.2f", product.precioCompra)}"
            tvPventa.text = "S/ ${String.format("%.2f", product.precioVenta)}"
            tvStock.text = "${product.stock} ${product.unidadMedida}"

            tvStatus.text = if (product.estado) "Disponible" else "No disponible"
            tvStatus.background = getDrawable(if (product.estado) R.drawable.rounded_tag_green else R.drawable.rounded_tag_grey)

            if (product.stock <= 5) {
                val colorAlert = ContextCompat.getColor(this@InventoryActivity, android.R.color.holo_red_dark)
                tvStock.setTextColor(colorAlert)
                tvStock.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
        val dialog = DialogHelper.createBaseDialog(this, binding.root)
        binding.btnEdit.setOnClickListener {
            dialog.dismiss()
            val editDialog = ProductFormDialogFragment.newInstance(product)
            editDialog.show(supportFragmentManager, "EditProduct")
        }

        binding.btnDelete.setOnClickListener {
            DialogHelper.showConfirmation(
                this,
                "Eliminar Producto",
                "¿Estás seguro de que deseas eliminar a ${product.nombre}?",
                onConfirm = {
                    viewModel.deleteProduct(product.id)
                    dialog.dismiss()
                })
        }

        binding.btnAceptar.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            val query = binding.etBuscar.text.toString()
            val esBúsqueda = query.isNotEmpty()

            val mensaje = if (esBúsqueda) "No se encontraron resultados para '$query'"
            else "No hay productos en esta categoría"

            // Aquí cambiamos la imagen: ic_search_off o similar
            mostrarMensajeOrLista(
                exito = true,
                lista = true,
                mensaje = mensaje,
                imagenRes = R.drawable.no_found // <--- Cambia esto por tu icono de búsqueda vacía
            )
        } else {
            mostrarMensajeOrLista(exito = true, lista = false)
        }
    }

    override fun onDestroy() {
        BottomSheetHelper.forceCleanup(this)
        super.onDestroy()
    }

    private fun setupCategoryChips(categories: List<Category>) {
        val chipGroup = binding.chipGroupCategories
        chipGroup.removeAllViews()

        // 1. Chip "Todos"
        val allChip = createChip("Todos", true)
        // No necesitamos poner el listener en el ChipGroup, lo pondremos en cada Chip
        chipGroup.addView(allChip)

        // 2. Chips de la DB
        categories.forEach { categoria ->
            val chip = createChip(categoria.nombre, false)
            chip.tag = categoria.id
            chipGroup.addView(chip)
        }
    }

    private fun createChip(label: String, isChecked: Boolean): Chip {
        return Chip(this).apply {
            id = View.generateViewId() // Es vital que tengan ID antes de cualquier otra cosa
            text = label
            isCheckable = true
            isCheckedIconVisible = false
            this.isChecked = isChecked

            setChipBackgroundColorResource(R.color.bg_chip_selector)
            setTextColor(ContextCompat.getColorStateList(context, R.color.text_chip_selector))
            elevation = 6f
            stateListAnimator = null

            // --- LÓGICA DE FILTRADO Y LIMPIEZA AQUÍ ---
            setOnCheckedChangeListener { _, isCheckedNow ->
                if (isCheckedNow) {
                    // 1. Limpiar el buscador si se selecciona una categoría específica
                    if (label.lowercase() != "todos") {
                        if (binding.etBuscar.text?.isNotEmpty() == true) {
                            // Usamos setText("") para limpiar el buscador
                            binding.etBuscar.setText("")
                        }
                    }

                    // 2. Ejecutar el filtro del adapter
                    val filterQuery = if (label.lowercase() == "todos") "" else label
                    adapter.filter.filter(filterQuery)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Esto simula presionar el botón físico/virtual de "atrás" del teléfono
        onBackPressedDispatcher.onBackPressed()
        return true
    }


}