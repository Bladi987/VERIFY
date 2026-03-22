package com.kasolution.verify.UI.Suppliers.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kasolution.verify.R
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.UI.Suppliers.viewModel.SuppliersViewModel
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.FragmentSuppliersFormDialogBinding

class SuppliersFormDialogFragment : DialogFragment() {
    private var _binding: FragmentSuppliersFormDialogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuppliersViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSuppliersFormDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val proveedor = arguments?.getParcelable<Supplier>(ARG_SUPPLIER)
        if (proveedor != null) {
            //MODO EDICION
            binding.tvDialogTitle.text = "Editar Proveedor"
            binding.btnSaveProv.setText("GUARDAR CAMBIOS")
            binding.etNombreProv.setText(proveedor.nombre)
            binding.etTelefonoProv.setText(proveedor.telefono)
            binding.etEmailProv.setText(proveedor.email)
            binding.etDireccionProv.setText(proveedor.direccion)
        }
        binding.btnSaveProv.setOnClickListener {
            val id = if (proveedor?.id != null) proveedor.id else 0
            val nombre = binding.etNombreProv.text.toString().trim()
            val telefono = binding.etTelefonoProv.text.toString().trim()
            val email = binding.etEmailProv.text.toString().trim()
            val direccion = binding.etDireccionProv.text.toString().trim()
            if (validar(nombre, telefono, email, direccion)) {
                if (proveedor != null) {
                    viewModel.updateSupplier(id, nombre,  telefono, email, direccion)
                } else {
                    viewModel.saveSupplier(nombre,  telefono, email, direccion)
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
            binding.btnSaveProv.setLoading(isLoading)
        }
    }
    private fun validar(n: String, t: String, e: String, di: String): Boolean {
        var isValid = true
        binding.run{
            tilNombreProv.error = null
            tilTelefonoProv.error = null
            tilEmailProv.error=null
            tilDireccionProv.error=null
        }
        if(n.isBlank()){ binding.tilNombreProv.error = "El nombre es obligatorio";isValid=false }
        if(t.isBlank()){ binding.tilTelefonoProv.error = "El telefono es obligatorio";isValid=false }
        if(e.isBlank()){ binding.tilEmailProv.error = "El email es obligatorio";isValid=false }
        if(di.isBlank()){ binding.tilDireccionProv.error = "La direccion es obligatoria";isValid=false }

        return isValid
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
        private const val ARG_SUPPLIER = "suppliers_data"

        fun newInstance(supplier: Supplier?): SuppliersFormDialogFragment {
            val fragment = SuppliersFormDialogFragment()
            supplier?.let {
                val args = Bundle()
                args.putParcelable(ARG_SUPPLIER, it)
                fragment.arguments = args
            }
            return fragment
        }
    }

}