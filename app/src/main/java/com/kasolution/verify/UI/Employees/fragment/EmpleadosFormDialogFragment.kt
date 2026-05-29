package com.kasolution.verify.UI.Employees.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kasolution.verify.R
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.core.utils.validate
import com.kasolution.verify.databinding.DialogSelectorRolesCardBinding
import com.kasolution.verify.databinding.FragmentEmpleadosFormDialogBinding
import com.kasolution.verify.domain.branch.model.Branch
import com.kasolution.verify.domain.role.model.Role


class EmpleadosFormDialogFragment : DialogFragment() {
    private var _binding: FragmentEmpleadosFormDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmpleadosViewModel by activityViewModels()
    //private val emp = arguments?.getParcelable<Employee>(ARG_EMPLEADO)
    private var emp: Employee? = null
    private var idSucursalSeleccionada: Int = 0
    private var nombreSucursalSeleccionada: String = ""
    private var listaSucursalesLocal: List<Branch> = emptyList()
    private var listaRolesLocal: List<Role> = emptyList()
    private var idRolSeleccionado: Int = 0
    private var nombreRolSeleccionado: String = ""
    private var slugRolSeleccionado: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmpleadosFormDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emp = arguments?.getParcelable<Employee>(ARG_EMPLEADO)

//        setupRoleDropdown()
        setupListener()
        setupObservers()
        initUIIfEditMode()
    }
    private fun initUIIfEditMode() {
        if (emp != null) {
            // MODO EDICIÓN
            binding.tvDialogTitle.text = "Editar Empleado"
            binding.btnSaveEmployee.setText("GUARDAR CAMBIOS")
            binding.etFullName.setText(emp!!.nombre)
            binding.etUsername.setText(emp!!.usuario)
            binding.swIsActive.isChecked = emp!!.estado

            binding.swIsActive.text = if (emp!!.estado) "Estado del Empleado (Activo)" else "Estado del Empleado (Inactivo)"
            binding.etEmail?.setText(emp!!.correo ?: "")
            binding.etPhone?.setText(emp!!.telefono ?: "")

            // Seteamos sucursal inicial en edición
            idSucursalSeleccionada = emp!!.idSucursalBase
            nombreSucursalSeleccionada = emp!!.sucursalNombre
            binding.actvSucursal.setText(nombreSucursalSeleccionada, false)

            // Seteamos el Rol Único inicial en edición
            idRolSeleccionado = emp!!.idRol
            nombreRolSeleccionado = emp!!.nombreRol
            slugRolSeleccionado = emp!!.rolSlug

            // Enlazamos al AutoCompleteTextView de roles (ajusta el ID según tu XML, ej: actvRoles)
            binding.actvRoles?.setText(nombreRolSeleccionado, false)
            binding.tilPasswordDialog.helperText = "Dejar en blanco si no deseas cambiarla"
        }else{
            binding.tvDialogTitle.text = "Configuración de Empleado"
            binding.swIsActive.text = "Estado del Empleado (Activo)"
            binding.tilPasswordDialog.helperText = "Requerido para el primer ingreso"
        }
    }
    private fun setupListener(){
        binding.btnSaveEmployee.setOnClickListener { saveEmployee() }
        binding.swIsActive.setOnCheckedChangeListener { _, isChecked ->
            binding.swIsActive.text = if (isChecked) "Estado del Empleado (Activo)" else "Estado del Empleado (Inactivo)"
        }
        binding.actvSucursal.setOnItemClickListener { parent, _, position, _ ->
            if (position in listaSucursalesLocal.indices) {
                val branch = listaSucursalesLocal[position]
                idSucursalSeleccionada = branch.id
                nombreSucursalSeleccionada = branch.nombre
                binding.tilSucursal.error = null
            }
        }
        binding.actvRoles?.setOnItemClickListener { parent, _, position, _ ->
            if (position in listaRolesLocal.indices) {
                val role = listaRolesLocal[position]
                idRolSeleccionado = role.id
                nombreRolSeleccionado = role.nombre
                slugRolSeleccionado = role.slug
                binding.tilRoles.error = null
            }
        }
    }
    private fun saveEmployee() {
        val isEdit = emp != null
        val id = emp?.id ?: 0
        val nombre = binding.etFullName.text.toString().trim()
        val usuario = binding.etUsername.text.toString().trim()
        val correo = binding.etEmail?.text.toString().trim().takeIf { it.isNotEmpty() }
        val telefono = binding.etPhone?.text.toString().trim().takeIf { it.isNotEmpty() }
        val pass = binding.etPassword.text.toString().trim()
        val pin = binding.etPin.text.toString().trim()
        val estado = binding.swIsActive.isChecked
        //val pin = "123456"

        if (validar(nombre, usuario, nombreSucursalSeleccionada, nombreRolSeleccionado, pass, isEdit)) {
            if (isEdit) {
                viewModel.updateEmpleado(
                    id = id,
                    nombre = nombre,
                    usuario = usuario,
                    correo = correo,
                    telefono = telefono,
                    pass = pass.takeIf { it.isNotEmpty() },
                    pin = pin.takeIf { it.isNotEmpty() },
                    idSucursal = idSucursalSeleccionada,
                    sucursalNombre = nombreSucursalSeleccionada,
                    idRol = idRolSeleccionado,
                    nombreRol = nombreRolSeleccionado,
                    rolSlug = slugRolSeleccionado,
                    estado = estado
                )
            } else {
                viewModel.saveEmpleado(
                    nombre = nombre,
                    usuario = usuario,
                    correo = correo,
                    telefono = telefono,
                    pass = pass,
                    pin = pin.takeIf { it.isNotEmpty() },
                    idSucursal = idSucursalSeleccionada,
                    sucursalNombre = nombreSucursalSeleccionada,
                    idRol = idRolSeleccionado,
                    nombreRol = nombreRolSeleccionado,
                    rolSlug = slugRolSeleccionado,
                    estado = estado
                )
            }
        }

    }

    private fun setupObservers() {
        // Escuchar si hay errores específicos al guardar
        viewModel.exception.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                ToastHelper.showCustomToast(binding.root, error, false)
            }
        }
        viewModel.sucuraslList.observe(viewLifecycleOwner) { sucursales ->
            if (sucursales != null) {
                listaSucursalesLocal = sucursales
                val nombresSucursales = sucursales.map { it.nombre }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nombresSucursales
                )
                binding.actvSucursal.setAdapter(adapter)
            }
        }
        viewModel.roleslList.observe(viewLifecycleOwner){ roles ->
            if (roles != null) {
                listaRolesLocal = roles
                val nombresRoles = roles.map { it.nombre }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    nombresRoles
                )
                binding.actvRoles?.setAdapter(adapter)
                Log.d("EmpleadosFormDialogFragment", "Nombres de roles mapeados: $nombresRoles")
            }
        }
        viewModel.operationSuccess.observe(viewLifecycleOwner) { action ->
            if (!action.isNullOrEmpty()) {
                dismiss()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveEmployee.setLoading(isLoading)
        }
    }

    private fun validar(nombre: String, usuario: String, sucursal: String,roles:String, pass: String, isEdit: Boolean): Boolean {
        with(binding) {
            val isNombreOk = tilFullName.validate(nombre.isBlank(), "El nombre es obligatorio")
            val isUsuarioOk = tilUserName.validate(usuario.isBlank(), "El usuario es obligatorio")
            val isSucursalOk = tilSucursal.validate(sucursal.isBlank(), "Seleccione una Sucursal")
            val isRolesOk = tilRoles.validate(roles.isBlank(), "Seleccione un Rol corporativo")
            val isPassOk = tilPasswordDialog.validate(!isEdit && pass.isBlank(), "Contraseña obligatoria")
            return isNombreOk && isUsuarioOk && isSucursalOk && isRolesOk && isPassOk
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