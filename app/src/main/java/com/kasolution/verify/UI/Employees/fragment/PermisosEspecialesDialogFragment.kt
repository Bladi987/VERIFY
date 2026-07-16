package com.kasolution.verify.UI.Employees.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Employees.adapter.EmployeePermissionsAdapter
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.core.utils.DialogHelper
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

    // --- 1. CONSTRUCCIÓN CON EL HELPER GLOBAL ---
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflamos el binding aquí para pasárselo al creador base
        _binding = DialogPermisosEspecialesBinding.inflate(layoutInflater)

        val dialog = DialogHelper.createBaseDialog(requireContext(), binding.root)
        // Bloqueo preventivo: evitar que se cierre al tocar afuera mientras se alteran permisos
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    // --- 2. RETORNO DE VISTA DIRECTO Y LIMPIO ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Ya no necesitamos configurar el fondo transparente o sin título aquí,
        // porque el DialogHelper ya lo hizo en el paso anterior.
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
        permissionsAdapter = EmployeePermissionsAdapter(emptyList()) { _, _ ->
            // Callback opcional por si necesitas activar estados visuales en tiempo real
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