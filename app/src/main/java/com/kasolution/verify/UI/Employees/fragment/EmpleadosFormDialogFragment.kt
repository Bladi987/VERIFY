package com.kasolution.verify.UI.Employees.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kasolution.verify.R
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.FragmentEmpleadosFormDialogBinding


class EmpleadosFormDialogFragment : DialogFragment() {
    private var _binding: FragmentEmpleadosFormDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmpleadosViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmpleadosFormDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emp = arguments?.getParcelable<Employee>(ARG_EMPLEADO)
        setupRoleDropdown()
        if (emp != null) {
            // MODO EDICIÓN
            binding.tvDialogTitle.text = "Editar Empleado"
            binding.btnSaveEmployee.setText("GUARDAR CAMBIOS")
            binding.etFullName.setText(emp.nombre)
            binding.etUsername.setText(emp.usuario)
            binding.actvRole.setText(emp.rol, false)
            binding.swIsActive.isChecked = emp.estado
        }
        binding.btnSaveEmployee.setOnClickListener {
            val isEdit = emp != null
            val id = if (emp?.id != null) emp.id else 0
            val nombre = binding.etFullName.text.toString().trim()
            val usuario = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val rol = binding.actvRole.text.toString()
            val isActive = binding.swIsActive.isChecked

            if (validar(nombre, usuario, pass, rol, isEdit)) {
                if (isEdit) {
                    val passUpdate = pass.takeIf { it.isNotEmpty() }
                    viewModel.updateEmpleado(id, nombre, usuario, passUpdate, rol, isActive)
                } else {
                    viewModel.saveEmpleado(nombre, usuario, pass, rol, isActive)

                }
            }
        }
        setupObservers()
    }

    private fun setupRoleDropdown() {
        val roles = resources.getStringArray(R.array.roles_array)

        // Importante: Usa 'android.R.layout.simple_list_item_1' para asegurar visibilidad
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roles)

        with(binding.actvRole) {
            setAdapter(adapter)

            // Esto fuerza a que se muestre el menú al hacer clic en cualquier parte del campo
            setOnClickListener { showDropDown() }

            // Evita que el usuario pueda escribir texto (solo seleccionar)
            inputType = android.text.InputType.TYPE_NULL
        }
    }

    private fun setupObservers() {
        // Escuchar si hay errores específicos al guardar
        viewModel.exception.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                ToastHelper.showCustomToast(binding.root, error, false)
//                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.operationSuccess.observe(viewLifecycleOwner) {action->
            if (!action.isNullOrEmpty()) {
                dismiss()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveEmployee.setLoading(isLoading)
        }
    }

    private fun validar(n: String, u: String, p: String, r: String, isEdit: Boolean): Boolean {
        // Limpiar errores previos (Opcional pero recomendado)
        binding.run {
            tilFullName.error = null
            tilUserName.error = null
            tilRole.error = null
            tilPasswordDialog.error = null
        }

        // Validaciones de campos obligatorios
        if (n.isBlank()) {
            binding.tilFullName.error = "El nombre es obligatorio"
            return false
        }

        if (u.isBlank()) {
            binding.tilUserName.error = "El usuario es obligatorio"
            return false
        }

        if (r.isBlank()) {
            binding.tilRole.error = "Seleccione un Rol"
            return false
        }

        // Validación condicional para nuevos registros
        if (!isEdit && p.isBlank()) {
            binding.tilPasswordDialog.error = "Contraseña es obligatoria para nuevos registros"
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
        private const val ARG_EMPLEADO = "empleado_data"

        fun newInstance(empleado: Employee? = null): EmpleadosFormDialogFragment {
            val fragment = EmpleadosFormDialogFragment()
            empleado?.let {
                val args = Bundle()
                args.putParcelable(ARG_EMPLEADO, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}