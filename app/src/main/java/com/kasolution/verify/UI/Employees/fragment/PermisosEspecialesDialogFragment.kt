package com.kasolution.verify.UI.Employees.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Employees.adapter.EmployeePermissionsAdapter
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.databinding.DialogPermisosEspecialesBinding

class PermisosEspecialesDialogFragment : DialogFragment() {

    private var _binding: DialogPermisosEspecialesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmpleadosViewModel by activityViewModels()
    private lateinit var permissionsAdapter: EmployeePermissionsAdapter

    private var empleadoId: Int = -1
    private var empleadoNombre: String = ""

    companion object {
        private const val ARG_EMPLEADO_ID = "arg_empleado_id"
        private const val ARG_EMPLEADO_NOMBRE = "arg_empleado_nombre"

        fun newInstance(id: Int, nombre: String): PermisosEspecialesDialogFragment {
            val fragment = PermisosEspecialesDialogFragment()
            val args = Bundle().apply {
                putInt(ARG_EMPLEADO_ID, id)
                putString(ARG_EMPLEADO_NOMBRE, nombre)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            empleadoId = it.getInt(ARG_EMPLEADO_ID, -1)
            empleadoNombre = it.getString(ARG_EMPLEADO_NOMBRE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPermisosEspecialesBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            requestFeature(Window.FEATURE_NO_TITLE)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmployeeSubtitle.text = "Empleado: $empleadoNombre"

        setupRecyclerView()
        setupObservers()

        viewModel.loadPermisosEspeciales(empleadoId)

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {

            val permisosDelAdapter = permissionsAdapter.getItems()

            val payloadExcepciones = permisosDelAdapter.map { permiso ->
                mapOf(
                    "id_permiso" to permiso.idPermiso,
                    "permitido" to permiso.excepcion
                )
            }

            Log.d("PermisosDialog", "Payload corregido que viaja al Socket: $payloadExcepciones")
            viewModel.savePermisosEspeciales(empleadoId, payloadExcepciones)
        }
    }

    private fun setupRecyclerView() {
        // Inicializamos con una lista vacía. Ya no requerimos mapeos complejos en el callback.
        permissionsAdapter = EmployeePermissionsAdapter(emptyList()) { _, _ ->
            // Puedes dejarlo vacío o usarlo para activar un botón de "Cambios sin guardar"
        }

        binding.rvPermissions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = permissionsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.permisosEspecialesList.observe(viewLifecycleOwner) { listaPermisos ->
            if (listaPermisos != null) {
                permissionsAdapter.updateList(listaPermisos)
            }
        }

        viewModel.savePermisosSuccess.observe(viewLifecycleOwner) { exito ->
            if (exito) {
                viewModel.resetSavePermisosStatus()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}