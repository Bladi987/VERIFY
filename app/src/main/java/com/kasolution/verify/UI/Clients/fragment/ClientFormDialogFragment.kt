package com.kasolution.verify.UI.Clients.fragment

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kasolution.verify.UI.Clientes.viewModel.ClientesViewModel
import com.kasolution.verify.UI.Clients.model.Cliente
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

        val cli = arguments?.getSerializable(ARG_CLIENT) as? Cliente

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
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
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
        if (n.isEmpty() || d.isEmpty() || t.isEmpty() || e.isEmpty() || di.isEmpty()) {
            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
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
        private const val ARG_CLIENT = "client_data"

        fun newInstance(cliente: Cliente?): ClientFormDialogFragment {
            val fragment = ClientFormDialogFragment()
            cliente?.let {
                val args = Bundle()
                args.putSerializable(ARG_CLIENT, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}