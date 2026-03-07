package com.kasolution.verify.UI.Inventory.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.UI.Components.Scanner.ScannerActivity
import com.kasolution.verify.UI.Inventory.adapter.GenericDropDownAdapter
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.UI.Inventory.viewModel.InventoryViewModel
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.FragmentProductFormDialogBinding

class ProductFormDialogFragment : DialogFragment() {
    private val TAG = "ProductFormDialogFragment"
    private var _binding: FragmentProductFormDialogBinding? = null
    private val binding get() = _binding!!
    private var idCategoriaSeleccionada: Int = 0
    private var idProveedorSeleccionado: Int = 0
    private var addCategorySheet: AddCategorySheet? = null
    private val viewModel: InventoryViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductFormDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val product = arguments?.getParcelable<Product>(ARG_PRODUCT)
        setupCurrencyFormatting(binding.etPrecioCompra)
        setupCurrencyFormatting(binding.etPrecioVenta)
        if (product != null) {
            //MODO EDICION
            binding.tvDialogTitle.text = "Modificar Producto"
            binding.btnSaveProduct.setText("GUARDAR CAMBIOS")
            binding.etCodigoProducto.setText(product.codigo)
            binding.etNombreProducto.setText(product.nombre)
            binding.etPrecioCompra.setText(String.format("%.2f", product.precioCompra))
            binding.etPrecioVenta.setText(String.format("%.2f", product.precioVenta))
            binding.spCategoria.setText(product.nombreCategoria)
            binding.spProveedor.setText(product.nombreProveedor)
            binding.etStockInicial.setText(product.stock.toString())
            binding.etUnidadMedida.setText(product.unidadMedida)
            idCategoriaSeleccionada = product.idCategoria
            idProveedorSeleccionado = product.idProveedor

        }
        binding.btnSaveProduct.setOnClickListener {
            val id = if (product?.id != null) product.id else 0
            val codigo = binding.etCodigoProducto.text.toString().trim()
            val nombre = binding.etNombreProducto.text.toString().trim()
            val precioCompra = binding.etPrecioCompra.text.toString().trim()
            val precioVenta = binding.etPrecioVenta.text.toString().trim()
            val categoria = binding.spCategoria.text.toString().trim()
            val proveedor = binding.spProveedor.text.toString().trim()
            val stockInicial = binding.etStockInicial.text.toString().trim()
            val unidadMedida = binding.etUnidadMedida.text.toString().trim()


            if (validar(
                    codigo,
                    nombre,
                    precioCompra,
                    precioVenta,
                    categoria,
                    proveedor,
                    stockInicial,
                    unidadMedida
                )
            ) {
                if (product != null) {
                    viewModel.updateProduct(
                        id = id,
                        codigo = codigo,
                        nombre = nombre,
                        idCategoria = idCategoriaSeleccionada,
                        idProveedor = idProveedorSeleccionado,
                        precioCompra = precioCompra.toDouble(),
                        precioVenta = precioVenta.toDouble(),
                        stock = stockInicial.toInt(),
                        unidadMedida = unidadMedida,
                        estado = true
                    )
                } else {
                    viewModel.saveProduct(
                        codigo = codigo,
                        nombre = nombre,
                        idCategoria = idCategoriaSeleccionada,
                        idProveedor = idProveedorSeleccionado,
                        precioCompra = precioCompra.toDouble(),
                        precioVenta = precioVenta.toDouble(),
                        stock = stockInicial.toInt(),
                        unidadMedida = unidadMedida,
                        estado = true
                    )
                }
            }
        }
        binding.tilCategoria.setEndIconOnClickListener {
            it.hideKeyboard()
            addCategorySheet = AddCategorySheet { nombre, descripcion ->
                // Llamamos al ViewModel para guardar la nueva categoría
                viewModel.saveCategory(nombre, descripcion)
            }
            addCategorySheet?.show(parentFragmentManager, "AddCategorySheet")
        }
        binding.tilCodigoProducto.setEndIconOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), ScannerActivity::class.java))
        }

        binding.spCategoria.apply {
            // 1. Al hacer clic directamente
            setOnClickListener {
                it.hideKeyboard()
            }

            // 2. Al recibir el foco desde otro campo (ej. usando el botón 'Siguiente' del teclado)
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.hideKeyboard()
                    // Esto asegura que la lista se despliegue de inmediato al llegar al campo
                    (view as? AutoCompleteTextView)?.showDropDown()
                }
            }
        }
        binding.spProveedor.apply {
            // 1. Al hacer clic directamente
            setOnClickListener {
                it.hideKeyboard()
            }

            // 2. Al recibir el foco desde otro campo (ej. usando el botón 'Siguiente' del teclado)
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.hideKeyboard()
                    // Esto asegura que la lista se despliegue de inmediato al llegar al campo
                    (view as? AutoCompleteTextView)?.showDropDown()
                }
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.suppliersList.observe(viewLifecycleOwner) { lista ->
            Log.d(TAG, "Lista de proveedores recibida: $lista")
            setupSelectorProveedor(lista)
        }
        viewModel.categoryList.observe(viewLifecycleOwner) { lista ->
            setupSelectorCategoria(lista)
            Log.d(TAG, "Lista de categorias recibida: $lista")
        }
        // Escuchar si hay errores específicos al guardar
        viewModel.exception.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                ToastHelper.showCustomToast(binding.root, error, false)
            }
        }
        viewModel.operationSuccess.observe(viewLifecycleOwner) { accion ->
            if (accion.isNullOrEmpty()) return@observe

            when (accion) {
                "CATEGORY_SAVE" -> {
                    addCategorySheet?.dismiss()
                    addCategorySheet = null
                    viewModel.resetOperationStatus() // Limpiar para que no afecte al diálogo padre
                }
                "PRODUCT_SAVE", "PRODUCT_UPDATE" -> {
                    // Toast de éxito antes de cerrar
                    ToastHelper.showCustomToast(binding.root, "Producto guardado con éxito", true)

                    // Limpiamos el estado ANTES de cerrar para que la siguiente vez que se abra esté limpio
                    viewModel.resetOperationStatus()
                    dismiss()
                }
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveProduct.setLoading(isLoading)
        }

    }

    private fun validar(
        codBar: String,
        nombre: String,
        pcompra: String,
        pventa: String,
        categoria: String,
        proveedor: String,
        sinicial: String,
        umedida: String
    ): Boolean {
        binding.run {
            tilCodigoProducto.error = null
            tilNombreProducto.error = null
            tilPrecioCompra.error = null
            tilPrecioVenta.error = null
            tilCategoria.error = null
            tilProveedor.error = null
            tilStockInicial.error = null
            tilUnidadMedida.error = null

        }
        if (codBar.isBlank()) {
            binding.tilCodigoProducto.error = "El codigo es obligatorio"
            return false
        }
        if (nombre.isBlank()) {
            binding.tilNombreProducto.error = "El nombre es obligatorio"
            return false
        }
        if (pcompra.isBlank()) {
            binding.tilPrecioCompra.error = "El precio de compra es obligatorio"
            return false
        }
        if (pventa.isBlank()) {
            binding.tilPrecioVenta.error = "El precio de venta es obligatorio"
            return false
        }
        if (categoria.isBlank()) {
            binding.tilCategoria.error = "La categoria es obligatoria"
            return false
        }
        if (proveedor.isBlank()) {
            binding.tilProveedor.error = "El proveedor es obligatorio"
            return false
        }
        if (sinicial.isBlank()) {
            binding.tilStockInicial.error = "El stock inicial es obligatorio"
            return false
        }
        if (umedida.isBlank()) {
            binding.tilUnidadMedida.error = "La unidad de medida es obligatoria"
            return false
        }
        return true
    }

    private fun setupSelectorProveedor(proveedores: List<Supplier>) {

        // Configurar Adaptador de Proveedores
        val provAdapter = GenericDropDownAdapter(requireContext(), proveedores) { it.nombre }
        binding.spProveedor.setAdapter(provAdapter)

        binding.spProveedor.setOnItemClickListener { parent, _, position, _ ->
            val supplier = parent.getItemAtPosition(position) as Supplier

            idProveedorSeleccionado = supplier.id // Capturamos el ID
            binding.tilProveedor.error = null
        }
    }

    private fun setupSelectorCategoria(categorias: List<Category>) {

        // Configurar Adaptador de Categorías
        val catAdapter = GenericDropDownAdapter(requireContext(), categorias) { it.nombre }
        binding.spCategoria.setAdapter(catAdapter)

        binding.spCategoria.setOnItemClickListener { parent, _, position, _ ->
            val category = parent.getItemAtPosition(position) as Category
            idCategoriaSeleccionada = category.id // Capturamos el ID
            binding.tilCategoria.error = null // Limpiamos errores visuales
        }
    }
    private val barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val codigoEscaneado = result.data?.getStringExtra("SCAN_RESULT")
            binding.etCodigoProducto.setText(codigoEscaneado) // Ponemos el código en el campo
        }
    }

    private fun setupCurrencyFormatting(editText: TextInputEditText) {
        val decimalFilter = InputFilter { source, _, _, dest, dstart, dend ->
            val builder = StringBuilder(dest)
            builder.replace(dstart, dend, source.toString())
            // RegEx: Solo números y máximo un punto con dos decimales
            if (!builder.toString().matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                if (source.isEmpty()) dest.subSequence(dstart, dend) else ""
            } else null
        }
        editText.filters = arrayOf(decimalFilter)
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    // Usamos toDoubleOrNull que es más idiomático en Kotlin
                    val parsed = text.toDoubleOrNull()
                    if (parsed != null) {
                        editText.setText(String.format("%.2f", parsed))
                    } else {
                        editText.text = null // Si es inválido (ej. solo un ".") limpiamos
                    }
                }
            } else {
                editText.selectAll()
            }
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Establecemos el ancho al 90% y el alto según el contenido
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Opcional: Quitar el fondo por defecto de Android para que se vea tu fondo redondeado
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        private const val ARG_PRODUCT = "product_data"

        fun newInstance(product: Product?): ProductFormDialogFragment {
            val fragment = ProductFormDialogFragment()
            product?.let {
                val args = Bundle()
                args.putParcelable(ARG_PRODUCT, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}