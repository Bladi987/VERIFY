package com.kasolution.verify.UI.Clients.fragment

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kasolution.verify.R
import com.kasolution.verify.UI.Clientes.viewModel.ClientesViewModel
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.FragmentClientFormDialogBinding


class ClientFormDialogFragment : DialogFragment() {
    private var _binding: FragmentClientFormDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClientesViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientFormDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cli = arguments?.getParcelable<Client>(ARG_CLIENT)

        if (cli != null) {
            //MODO EDICION
            binding.tvDialogTitle.text = "Editar Cliente"
            binding.btnSaveClient.setText("GUARDAR CAMBIOS")
            binding.etFullName.setText(cli.nombre)
            binding.etDniRuc.setText(cli.dniRuc)
            binding.etTelefono.setText(cli.telefono)
            binding.etEmail.setText(cli.email)
            binding.etDireccion.setText(cli.direccion)
        }
        binding.btnSaveClient.setOnClickListener {
            val id = if (cli?.id != null) cli.id else 0
            val nombre = binding.etFullName.text.toString().trim()
            val dniRuc = binding.etDniRuc.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val direccion = binding.etDireccion.text.toString().trim()
            if (validar(nombre, dniRuc, telefono, email, direccion)) {
                if (cli != null) {
                    viewModel.updateCliente(id, nombre, dniRuc, telefono, email, direccion)
                } else {
                    viewModel.saveCliente(nombre, dniRuc, telefono, email, direccion)
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
            binding.btnSaveClient.setLoading(isLoading)
        }

    }
    private fun validar(n: String, d: String, t: String, e: String, di: String): Boolean {
        var isValid = true
        binding.run{
            tilFullName.error = null
            tilDniRuc.error = null
            tilTelefono.error = null
            tilEmail.error=null
            tilDireccion.error=null
        }
        if(n.isBlank()){ binding.tilFullName.error = "El nombre es obligatorio";isValid=false }
        if(d.isBlank()){ binding.tilDniRuc.error = "El DNI/RUC es obligatorio";isValid=false }
        if(t.isBlank()){ binding.tilTelefono.error = "El telefono es obligatorio";isValid=false }
        if(e.isBlank()){ binding.tilEmail.error = "El email es obligatorio";isValid=false }
        if(di.isBlank()){ binding.tilDireccion.error = "La direccion es obligatoria";isValid=false }

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
        private const val ARG_CLIENT = "client_data"

        fun newInstance(cliente: Client?): ClientFormDialogFragment {
            val fragment = ClientFormDialogFragment()
            cliente?.let {
                val args = Bundle()
                args.putParcelable(ARG_CLIENT, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}