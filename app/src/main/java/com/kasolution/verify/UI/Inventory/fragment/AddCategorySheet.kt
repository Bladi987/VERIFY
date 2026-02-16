package com.kasolution.verify.UI.Inventory.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kasolution.verify.R
import com.kasolution.verify.databinding.LayoutAddCategorySheetBinding

class AddCategorySheet(
    private val onSave: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutAddCategorySheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutAddCategorySheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuardarCategoria.setOnClickListener {
            val nombre = binding.etNombreCat.text.toString().trim()
            val descripcion = binding.etDescCat.text.toString().trim()

            if (nombre.isEmpty()) {
                binding.tilNombreCat.error = "El nombre es obligatorio"
                return@setOnClickListener
            }
            binding.btnGuardarCategoria.setLoading(true)
            onSave(nombre, descripcion)
            //dismiss() // Cierra el panel después de enviar
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Opcional: Quitar el fondo por defecto de Android para que se vea tu fondo redondeado
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}