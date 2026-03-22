package com.kasolution.verify.UI.Inventory.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.kasolution.verify.R
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
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val product = arguments?.getParcelable<Product>(ARG_PRODUCT)
        setupCurrencyFormatting(binding.etPrecioCompra)
        setupCurrencyFormatting(binding.etPrecioVenta)
        if (product != null) {
            //MODO EDICION
            binding.tvDialogTitle.text = "Modificar Producto"
            binding.btnSaveProduct.setText("GUARDAR CAMBIOS")

            product.let {
                binding.etCodigoProducto.setText(it.codigo)
                binding.etNombreProducto.setText(it.nombre)
                binding.etPrecioCompra.setText(String.format("%.2f", it.precioCompra))
                binding.etPrecioVenta.setText(String.format("%.2f", it.precioVenta))
                binding.spCategoria.setText(it.nombreCategoria, false)
                binding.spProveedor.setText(it.nombreProveedor, false)
                binding.etStockInicial.setText(it.stock.toString())
                binding.etUnidadMedida.setText(it.unidadMedida)
                idCategoriaSeleccionada = it.idCategoria
                idProveedorSeleccionado = it.idProveedor
            }

        }
        binding.btnSaveProduct.setOnClickListener {
            prepararYEnviar(product)
        }
        binding.tilCategoria.setEndIconOnClickListener {
            it.setKeyboardVisibility(false)
            abrirNuevaCategoria()
        }
        binding.tilCodigoProducto.setEndIconOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), ScannerActivity::class.java))
        }

        setupDropdownFocus()
        setupObservers()
    }

    private fun prepararYEnviar(existingProduct: Product?) {
        val codigo = binding.etCodigoProducto.text.toString().trim()
        val nombre = binding.etNombreProducto.text.toString().trim()
        val pCompra = binding.etPrecioCompra.text.toString().trim()
        val pVenta = binding.etPrecioVenta.text.toString().trim()
        val categoria = binding.spCategoria.text.toString().trim()
        val proveedor = binding.spProveedor.text.toString().trim()
        val stock = binding.etStockInicial.text.toString().trim()
        val unidad = binding.etUnidadMedida.text.toString().trim()

        if (validar(codigo, nombre, pCompra, pVenta, categoria, proveedor, stock, unidad)) {
            // Creamos el objeto de dominio directamente (como en Clientes)
            val productToSave = Product(
                id = existingProduct?.id ?: 0,
                codigo = codigo,
                nombre = nombre,
                idCategoria = idCategoriaSeleccionada,
                idProveedor = idProveedorSeleccionado,
                precioCompra = pCompra.toDoubleOrNull() ?: 0.0,
                precioVenta = pVenta.toDoubleOrNull() ?: 0.0,
                stock = stock.toIntOrNull() ?: 0,
                unidadMedida = unidad,
                estado = true // O el estado que desees manejar
            )

            if (existingProduct != null) {
                viewModel.updateProduct(productToSave)
            } else {
                viewModel.saveProduct(productToSave)
            }
        }
    }
    private fun abrirNuevaCategoria() {
        addCategorySheet = AddCategorySheet { nombre, descripcion ->
            viewModel.saveCategory(nombre, descripcion)
        }
        addCategorySheet?.show(parentFragmentManager, "AddCategorySheet")
    }
    private fun setupDropdownFocus() {
        listOf(binding.spCategoria, binding.spProveedor).forEach { spinner ->
            spinner.setOnClickListener { it.setKeyboardVisibility(false) }
            spinner.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.setKeyboardVisibility(false)
                    (v as? AutoCompleteTextView)?.showDropDown()
                }
            }
        }
    }
    private fun setupSelectorProveedor(proveedores: List<Supplier>) {
        val adapter = GenericDropDownAdapter(requireContext(), proveedores) { it.nombre }
        binding.spProveedor.setAdapter(adapter)
        binding.spProveedor.setOnItemClickListener { parent, _, pos, _ ->
            idProveedorSeleccionado = (parent.getItemAtPosition(pos) as Supplier).id
            binding.tilProveedor.error = null
        }
    }

    private fun setupSelectorCategoria(categorias: List<Category>) {
        val adapter = GenericDropDownAdapter(requireContext(), categorias) { it.nombre }
        binding.spCategoria.setAdapter(adapter)
        binding.spCategoria.setOnItemClickListener { parent, _, pos, _ ->
            idCategoriaSeleccionada = (parent.getItemAtPosition(pos) as Category).id
            binding.tilCategoria.error = null
        }
    }


    private fun setupObservers() {
        // Listas para Spinners
        viewModel.suppliersList.observe(viewLifecycleOwner) { setupSelectorProveedor(it) }
        viewModel.categoryList.observe(viewLifecycleOwner) { setupSelectorCategoria(it) }

        // Estado de carga y errores
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.btnSaveProduct.setLoading(it) }

        viewModel.exception.observe(viewLifecycleOwner) {
            dismiss()
        }

        // Éxito de operación (Alineado con Clientes)
        viewModel.operationSuccess.observe(viewLifecycleOwner) { accion ->
            if (accion.isNullOrEmpty()) return@observe

            when (accion) {
                "CATEGORY_SAVE" -> {
                    addCategorySheet?.dismiss()
                    addCategorySheet = null
                    // No reseteamos el estado aquí para permitir que el flujo principal continúe
                }
                "PRODUCT_SAVE", "PRODUCT_UPDATE" -> {
                    ToastHelper.showCustomToast(binding.root, "Producto guardado", true)
                    viewModel.resetOperationStatus()
                    dismiss()
                }
            }
        }
    }

    private fun validar(cod: String, nom: String, pc: String, pv: String, cat: String, prov: String, st: String, un: String): Boolean {
        var isValid = true
        // Limpiar errores
        binding.run {
            tilCodigoProducto.error = null; tilNombreProducto.error = null
            tilPrecioCompra.error = null; tilPrecioVenta.error = null
            tilCategoria.error = null; tilProveedor.error = null
            tilStockInicial.error = null; tilUnidadMedida.error = null
        }

        if (cod.isBlank()) { binding.tilCodigoProducto.error = "Obligatorio"; isValid = false }
        if (nom.isBlank()) { binding.tilNombreProducto.error = "Obligatorio"; isValid = false }
        if (pc.isBlank()) { binding.tilPrecioCompra.error = "Obligatorio"; isValid = false }
        if (pv.isBlank()) { binding.tilPrecioVenta.error = "Obligatorio"; isValid = false }
        if (cat.isBlank()) { binding.tilCategoria.error = "Obligatorio"; isValid = false }
        if (prov.isBlank()) { binding.tilProveedor.error = "Obligatorio"; isValid = false }
        if (st.isBlank()) { binding.tilStockInicial.error = "Obligatorio"; isValid = false }
        if (un.isBlank()) { binding.tilUnidadMedida.error = "Obligatorio"; isValid = false }

        return isValid
    }

    private val barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val codigoEscaneado = result.data?.getStringExtra("SCAN_RESULT")
            binding.etCodigoProducto.setText(codigoEscaneado) // Ponemos el código en el campo
            binding.etNombreProducto.requestFocus()
            binding.etNombreProducto.postDelayed({
                binding.etNombreProducto.setKeyboardVisibility(show = true)
            }, 150)
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
    fun View.setKeyboardVisibility(show: Boolean) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            this.requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        } else {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
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
            setWindowAnimations(R.style.AnimationiOSDialog)
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