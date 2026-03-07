package com.kasolution.verify.UI.Inventory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.kasolution.verify.R
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Inventory.adapter.InventoryAdapter
import com.kasolution.verify.UI.Inventory.fragment.ProductFormDialogFragment
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.UI.Inventory.viewModel.InventoryViewModel
import com.kasolution.verify.core.AppProvider
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

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val viewModel: InventoryViewModel by viewModels {
        AppProvider.provideInventoryViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutOptions)
        bottomSheetBehavior.apply {
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN // Inicia oculto
        }
        //zona para configurar el toolBar
//        setSupportActionBar(binding.actionBar)
//        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
//            setDisplayShowHomeEnabled(true)
//        }
        //------------------------------
        lista = ArrayList()
        initRecycler()
        initBottonSheet()
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
                        // Al marcarlo, el listener del ChipGroup se activará,
                        // pero como el texto no está vacío, el filtro final será el del buscador.
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

        binding.btnEditOption.setOnClickListener {
            selectedProduct?.let { product ->
                // Abrimos el diálogo enviando el objeto producto
                val dialog = ProductFormDialogFragment.newInstance(product)
                dialog.show(supportFragmentManager, "EditProduct")
                hideOptions()
            }
        }

        binding.btnDeleteOption.setOnClickListener {
            selectedProduct?.let { product ->
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Producto",
                    "¿Estás seguro de que deseas eliminar a ${product.nombre}?",
                    onConfirm = {
                        viewModel.deleteProduct(product.id)
                        hideOptions()
                    })
            }
        }
        binding.tilCodigoProducto.setEndIconOnClickListener {
//            barcodeLauncher.launch(Intent(this, ScannerActivity::class.java))
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("SINGLE_SCAN", true) // ACTIVAR MODO MULTI
            scannerLauncher.launch(intent)
        }

// En el listener de los Chips



        binding.btnRetry.setOnClickListener {
            binding.etBuscar.setText("")
            // Opcional: Marcar chip "Todos"
            (binding.chipGroupCategories.getChildAt(0) as? Chip)?.isChecked = true
            viewModel.loadProducts()
        }

//
//        binding.btnSearch.setOnClickListener {
//            alternarTitulo()
//        }

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

//        adapter.onDataChanged(lista.isEmpty())
    }

    private fun initBottonSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Aquí puedes realizar acciones cuando el usuario termine de deslizarlo hacia abajo
                    selectedProduct = null
                    adapter.clearSelection()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Opcional: puedes cambiar la opacidad de un fondo oscuro aquí
            }
        })
    }

    private fun setupObservers() {
        viewModel.productsList.observe(this) { lista ->
            val emptyList = lista.isNullOrEmpty()
            mostrarMensajeOrLista(true, emptyList)
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.categoryList.observe(this) { lista ->
            setupCategoryChips(lista)
        }
        viewModel.exception.observe(this) { error ->
            mostrarMensajeOrLista(exito = false, mensaje = error)
            binding.btnRetry.isEnabled = true
            ToastHelper.showCustomToast(binding.root, error, false)
        }
        viewModel.operationSuccess.observe(this) { accion ->
            if (accion.isNullOrEmpty()) return@observe
            val mensaje = when (accion) {
                "PRODUCT_SAVE" -> "¡Producto registrado con éxito!"
                "PRODUCT_UPDATE" -> "Datos actualizados correctamente"
                "PRODUCT_DELETE" -> "Producto eliminado"
                "CATEGORY_SAVE" -> "Categoría registrada con éxito!"
                else -> ""
            }
            ToastHelper.showCustomToast(binding.root, mensaje, true)
            binding.root.postDelayed({
                viewModel.resetOperationStatus()
            }, 100)
        }

        viewModel.isLoading.observe(this) { loading ->
            ProgressHelper.showProgress(this, "Cargando...")
            if (!loading) ProgressHelper.hideProgress()
        }
    }

    private fun onItemClicListener(product: Product) {
        if (selectedProduct != null) hideOptions() else dialogShowProduct(product)
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
        binding.etBuscar.clearFocus()
        selectedProduct = product
        binding.tvSelectedName.text = product.nombre
        adapter.setSelectedItem(position)

        if (binding.layoutOptions.visibility != View.VISIBLE) {
            binding.layoutOptions.visibility = View.VISIBLE
        }

        // En lugar de usar .animate(), usamos los estados del Behavior
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            // Animación sutil de feedback si ya estaba abierto (cambio de empleado)
            binding.tvSelectedName.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100)
                .withEndAction {
                    binding.tvSelectedName.animate().scaleX(1f).scaleY(1f).start()
                }.start()
        }
    }

    private fun hideOptions() {
        adapter.clearSelection()
        selectedProduct = null
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun mostrarMensajeOrLista(
        exito: Boolean,
        lista: Boolean = false,
        mensaje: String = "Sin datos que mostrar",
        imagenRes: Int = R.drawable.no_signal // <--- Imagen por defecto
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
        // Cargar animación
        val anim1 = AnimationUtils.loadAnimation(this, R.anim.bounce)
        // Usar ViewBinding para inflar el layout del diálogo
        val binding = ItemCardInventoryShowBinding.inflate(LayoutInflater.from(this))
        // Análisis y precarga
        // Modificar el nombre de la hoja
        binding.tvCodigo.text = product.codigo
        binding.tvNombre.text = product.nombre
        binding.tvCategoria.text = "${product.nombreCategoria}"
        binding.tvProveedor.text = "${product.nombreProveedor}"
        binding.tvPcompra.text = "S/ ${String.format("%.2f", product.precioCompra)}"
        binding.tvPventa.text = "S/ ${String.format("%.2f", product.precioVenta)}"
        binding.tvStock.text = "${product.stock} ${product.unidadMedida}"
        val estado = if (product.estado) "Disponible" else "No disponible"

        if (product.estado)
            binding.tvStatus.background = getDrawable(R.drawable.rounded_tag_green)
        else {            // Estado INACTIVO: Fondo gris o rojo (Asumiendo un color gris para inactivo)
            binding.tvStatus.background = getDrawable(R.drawable.rounded_tag_grey)
        }
        if (product.stock <= 5) {
            // Color Rojo para alerta
            val colorAlert = ContextCompat.getColor(this, android.R.color.holo_red_dark)
            binding.tvStock.setTextColor(colorAlert)
            binding.tvStock.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        binding.tvStatus.text = estado


        // Crear el diálogo y mostrarlo
        val builder = AlertDialog.Builder(this)
        builder.setView(binding.root)
        val dialog = builder.create()
        dialog.show()
        // Establecer animación
//        binding.root.startAnimation(anim1)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        binding.btnEdit.setOnClickListener {
            dialog.dismiss()
//            ToastHelper.showCustomToast(binding.root, "Editar", true)
            val dialog = ProductFormDialogFragment.newInstance(product)
            dialog.show(supportFragmentManager, "EditProduct")
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
        // Lógica de los botones
        binding.btnAceptar.setOnClickListener {
            dialog.dismiss()
        }

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