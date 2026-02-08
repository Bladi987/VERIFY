package com.kasolution.verify.UI.Employees

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.kasolution.verify.R
import com.kasolution.verify.UI.Employees.adapter.EmpleadosAdapter
import com.kasolution.verify.UI.Employees.fragment.EmpleadosFormDialogFragment
import com.kasolution.verify.UI.Employees.model.Empleado
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityEmpleadosBinding

class EmployeesActivity : AppCompatActivity() {
    private val TAG = "EmpleadosActivity"
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: EmpleadosAdapter
    private lateinit var lista: ArrayList<Empleado>
    private var selectedEmpleado: Empleado? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var binding: ActivityEmpleadosBinding

    // Inyección del ViewModel usando el Factory del AppProvider
    private val viewModel: EmpleadosViewModel by viewModels {
        AppProvider.provideEmpleadosViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 1. Setup del Behavior
        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutOptions)
        bottomSheetBehavior.apply {
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN // Inicia oculto
        }

        setSupportActionBar(binding.actionBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        lista = ArrayList()
        initRecycler()
        initBottonSheet() // Este método ya configura el callback
        setupObservers()
        viewModel.loadEmpleados()

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

        binding.btnEditOption.setOnClickListener {
            selectedEmpleado?.let { emp ->
                val dialog = EmpleadosFormDialogFragment.newInstance(emp)
                dialog.show(supportFragmentManager, "EditEmpleado")
                hideOptions()
            }
        }

        binding.btnDeleteOption.setOnClickListener {
            selectedEmpleado?.let { emp ->
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Cuenta",
                    "¿Estás seguro de que deseas eliminar a ${emp.nombre}?",
                    onConfirm = {
                        viewModel.deleteEmpleado(emp.id)
                        hideOptions()
                    })

//                AlertDialog.Builder(this)
//                    .setTitle("Eliminar Cuenta")
//                    .setMessage("¿Estás seguro de que deseas eliminar a ${emp.nombre}?")
//                    .setPositiveButton("Eliminar") { _, _ ->
//                        viewModel.deleteEmpleado(emp.id)
//                        hideOptions()
//                    }
//                    .setNegativeButton("Cancelar", null)
//                    .show()
            }
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

    private fun initBottonSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Aquí puedes realizar acciones cuando el usuario termine de deslizarlo hacia abajo
                    adapter.clearSelection()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Opcional: puedes cambiar la opacidad de un fondo oscuro aquí
            }
        })
    }

    private fun onItemClicListener(empleado: Empleado) {
        //Toast.makeText(this, "${empleado.nombre} - ${empleado.usuario}", Toast.LENGTH_SHORT).show()
        hideOptions()
    }

    private fun showOptionsFor(empleado: Empleado, position: Int) {
        selectedEmpleado = empleado
        binding.tvSelectedName.text = empleado.nombre
        adapter.setSelectedItem(position)

        // IMPORTANTE: Aseguramos que sea visible para que el Behavior pueda animarlo
        if (binding.layoutOptions.visibility != View.VISIBLE) {
            binding.layoutOptions.visibility = View.VISIBLE
        }

        // En lugar de usar .animate(), usamos los estados del Behavior
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            // Animación sutil de feedback si ya estaba abierto (cambio de empleado)
            binding.tvSelectedName.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100)
                .withEndAction {
                    binding.tvSelectedName.animate().scaleX(1f).scaleY(1f).start()
                }.start()
        }
    }

    private fun hideOptions() {
        adapter.clearSelection()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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