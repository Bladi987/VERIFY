package com.kasolution.verify.UI.Employees

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kasolution.verify.R
import com.kasolution.verify.UI.Clients.fragment.ClientFormDialogFragment
import com.kasolution.verify.UI.Employees.adapter.EmpleadosAdapter
import com.kasolution.verify.UI.Employees.fragment.EmpleadosFormDialogFragment
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.BottomSheetHelper
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityEmpleadosBinding

class EmployeesActivity : AppCompatActivity() {
    private val TAG = "EmpleadosActivity"
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: EmpleadosAdapter
    private lateinit var lista: ArrayList<Employee>
    private var selectedEmpleado: Employee? = null
    private lateinit var binding: ActivityEmpleadosBinding

    // Inyección del ViewModel usando el Factory del AppProvider
    private val viewModel: EmpleadosViewModel by viewModels {
        AppProvider.provideEmpleadosViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.actionBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        lista = ArrayList()
        initRecycler()
        setupObservers()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ejecutar el filtro cada vez que el texto cambie
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnAddEmployee.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = EmpleadosFormDialogFragment()
            // El "EmpleadoTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "EmpleadoTag")
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadEmpleados()
        }

        binding.btnSearch.setOnClickListener {
            alternarTitulo()
        }
    }


    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = EmpleadosAdapter(
            listaInicial = lista,
            onClickListener = { empleado -> onItemClicListener(empleado) },
            onLongClickListener = { empleado, position -> showOptionsFor(empleado, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvEmployees.layoutManager = lmanager
        binding.rvEmployees.adapter = adapter

        adapter.onDataChanged(lista.isEmpty())
    }


    private fun onItemClicListener(empleado: Employee) {
        //Toast.makeText(this, "${empleado.nombre} - ${empleado.usuario}", Toast.LENGTH_SHORT).show(
    }

    private fun showOptionsFor(empleado: Employee, position: Int) {
        binding.etSearch.clearFocus()
        selectedEmpleado = empleado
        adapter.setSelectedItem(position)
        BottomSheetHelper.showInventoryOptions(
            activity = this,
            cabeceraName = "Empleado",
            name = empleado.nombre,
            onEdit = {
                val dialog = EmpleadosFormDialogFragment.newInstance(empleado)
                dialog.show(supportFragmentManager, "EditEmpleado")
            },
            onDelete = {
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Cuenta",
                    "¿Estás seguro de que deseas eliminar a ${empleado.nombre}?",
                    onConfirm = {
                        viewModel.deleteEmpleado(empleado.id)
                    })
            },
            onDismiss = {
                // Limpieza visual cuando el menú se va
                selectedEmpleado = null
                adapter.clearSelection()
            }
        )
    }
    private fun setupObservers() {
        viewModel.empleadosList.observe(this) { lista ->
            // El Observer solo entrega la data.
            // El adaptador internamente dirá "estoy vacío" o "tengo datos"
            val emptyList = lista.isNullOrEmpty()
            mostrarMensajeOrLista(true, emptyList)
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.exception.observe(this) { error ->
            // Manejar el error según tus necesidades
            Log.d(TAG, "error: $error")
            mostrarMensajeOrLista(exito = false, mensaje = error)
            ToastHelper.showCustomToast(binding.root, error, false)
        }
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "EMPLEADO_SAVE" -> "¡Empleado registrado con éxito!"
                "EMPLEADO_UPDATE" -> "Datos actualizados correctamente"
                "EMPLEADO_DELETE" -> "Empleado eliminado"
                else -> ""
            }
            ToastHelper.showCustomToast(binding.root, mensaje, true)
            binding.root.postDelayed({
                viewModel.resetOperationStatus()
            }, 100)
        }
        viewModel.isLoading.observe(this) { loading ->
            Log.d(TAG, "isLoading: $loading")
            ProgressHelper.showProgress(this, "Cargando...")
            if (!loading) ProgressHelper.hideProgress()
        }
    }

    fun mostrarMensajeOrLista(
        exito: Boolean,
        lista: Boolean = false,
        mensaje: String = "Sin datos que mostrar"
    ) {
        if (exito) {
            //la peticion fue exitosa, ahora validamos si la lista esta vacio
            if (lista) {
                //si la lista esta vacio mostramos el mensaje
                binding.rvEmployees.isVisible = false
                binding.layoutMessage.isVisible = true
                binding.tvTitleMeassage.text = mensaje
                binding.btnRetry.isVisible = false
                binding.btnAddEmployee.isEnabled = true
                binding.btnSearch.isEnabled = true
            } else {
                //mostramos el recyclerView
                binding.rvEmployees.isVisible = true
                binding.layoutMessage.isVisible = false
                binding.btnAddEmployee.isEnabled = true
                binding.btnSearch.isEnabled = true
            }
        } else {
            //mostramos mensaje ya sea sin datos o error
            binding.rvEmployees.isVisible = false
            binding.layoutMessage.isVisible = true
            binding.tvTitleMeassage.text = mensaje
            binding.btnRetry.isVisible = true
            binding.btnAddEmployee.isEnabled = false
            binding.btnSearch.isEnabled = false
        }

    }

    fun alternarTitulo() {
        val animInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        val animOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_top)
        val animInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        val animOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)
        if (binding.etSearch.visibility == View.VISIBLE) {
            binding.etSearch.startAnimation(animOutRight)
            binding.ivSearch.setImageResource(R.drawable.ic_search)
            binding.etSearch.text?.clear()
            binding.etSearch.visibility = View.GONE
            binding.tvTitle.visibility = View.VISIBLE
            binding.tvTitle.startAnimation(animInLeft)
        } else {
            binding.tvTitle.startAnimation(animOutLeft)
            binding.tvTitle.visibility = View.GONE
            binding.etSearch.startAnimation(animInRight)
            binding.etSearch.visibility = View.VISIBLE
            binding.ivSearch.setImageResource(R.drawable.ic_close)
            binding.etSearch.requestFocus()
            val inputMethodManager =
                this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        binding.rvEmployees.isVisible = !isEmpty
        //binding.tvEmptyState.isVisible = isEmpty
    }

    override fun onSupportNavigateUp(): Boolean {
        // Esto simula presionar el botón físico/virtual de "atrás" del teléfono
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}