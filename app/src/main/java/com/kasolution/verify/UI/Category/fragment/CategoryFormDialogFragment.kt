package com.kasolution.verify.UI.Category.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.UI.Category.viewModel.CategoriesViewModel
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.LayoutAddCategorySheetBinding

class CategoryFormDialogFragment : DialogFragment() {
    private var _binding: LayoutAddCategorySheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutAddCategorySheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val category = arguments?.getSerializable(ARG_CATEGORY) as? Category
        if (category != null) {
            //MODO EDICION
            binding.tvDialogTitle.text = "Editar Categoria"
            binding.btnGuardarCategoria.setText("GUARDAR CAMBIOS")
            binding.etNombreCat.setText(category.nombre)
            binding.etDescCat.setText(category.descripcion)
            binding.swIsActive.isChecked = category.estado
            binding.swIsActive.isVisible=true

        }
        binding.btnGuardarCategoria.setOnClickListener {
            val id = if (category?.id != null) category.id else 0
            val nombre = binding.etNombreCat.text.toString().trim()
            val descripcion = binding.etDescCat.text.toString().trim()
            val isActive = binding.swIsActive.isChecked
            if (validar(nombre)) {
                if (nombre != null) {
                    viewModel.updateCategory(id, nombre, descripcion, true)
                } else {
                    viewModel.saveCategory(nombre, descripcion, isActive)
                }
            }
        }
        setupObservers()
    }

    private fun setupObservers() {
        // Escuchar si hay errores específicos al guardar
        viewModel.exception.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                ToastHelper.showCustomToast(binding.root, error, false)
            }
        }
        viewModel.operationSuccess.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) { // Verificamos que no sea el valor reseteado
                dismiss()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnGuardarCategoria.setLoading(isLoading)
        }

    }
    private fun validar(nombre: String): Boolean {
        binding.run{
            tilNombreCat.error = null
        }
        if(nombre.isBlank()){
            binding.tilNombreCat.error = "El nombre es obligatorio"
            return false
        }
        return true
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
        private const val ARG_CATEGORY = "category_data"

        fun newInstance(category: Category?): CategoryFormDialogFragment {
            val fragment = CategoryFormDialogFragment()
            category?.let {
                val args = Bundle()
                args.putSerializable(ARG_CATEGORY, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}