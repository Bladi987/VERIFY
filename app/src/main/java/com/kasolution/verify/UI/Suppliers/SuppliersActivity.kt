package com.kasolution.verify.UI.Suppliers

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Suppliers.adapter.SupplierAdapter
import com.kasolution.verify.UI.Suppliers.fragment.SuppliersFormDialogFragment
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.UI.Suppliers.viewModel.SuppliersViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.BottomSheetHelper
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper


import com.kasolution.verify.databinding.ActivitySuppliersBinding
import kotlin.getValue

class SuppliersActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuppliersBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: SupplierAdapter
    private lateinit var lista: ArrayList<Supplier>
    private var selectedSupplier: Supplier? = null
    private val viewModel: SuppliersViewModel by viewModels {
        AppProvider.provideSuppliersViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivitySuppliersBinding.inflate(layoutInflater)
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
        binding.btnAddProveedor.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = SuppliersFormDialogFragment()
            // El "SupplierTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "SupplierTag")
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadSuppliers()
        }

        binding.btnSearch.setOnClickListener {
            alternarTitulo()
        }
    }
    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = SupplierAdapter(
            listaInicial = lista,
            onClickListener = { supplier -> onItemClicListener(supplier) },
            onLongClickListener = { supplier, position -> showOptionsFor(supplier, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvSuppliers.layoutManager = lmanager
        binding.rvSuppliers.adapter = adapter

        adapter.onDataChanged(lista.isEmpty())
    }
    private fun onItemClicListener(supplier: Supplier) {
        //Toast.makeText(this, "${supplier.nombre} - ${supplier.usuario}", Toast.LENGTH_SHORT).show()
    }
    private fun showOptionsFor(supplier: Supplier, position: Int) {
        binding.etSearch.clearFocus()
        selectedSupplier = supplier
        adapter.setSelectedItem(position)

        BottomSheetHelper.showInventoryOptions(
            activity = this,
            cabeceraName = "Proveedor",
            name = supplier.nombre,
            onEdit = {
                val dialog = SuppliersFormDialogFragment.newInstance(supplier)
                dialog.show(supportFragmentManager, "EditSupplier")
            },
            onDelete = {
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Proveedor",
                    "¿Estás seguro de que deseas eliminar a ${supplier.nombre}?",
                    onConfirm = {
                        viewModel.deleteSupplier(supplier.id)
                    })
            },
            onDismiss = {
                // Limpieza visual cuando el menú se va
                selectedSupplier = null
                adapter.clearSelection()
            }
        )

    }

    private fun setupObservers() {
        viewModel.suppliersList .observe(this) { lista ->
            val emptyList = lista.isNullOrEmpty()
            mostrarMensajeOrLista(true, emptyList)
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.exception.observe(this) { error ->
            mostrarMensajeOrLista(exito = false, mensaje = error)
            ToastHelper.clasicCustomToast(binding.root, error, false)
        }
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "SUPPLIER_SAVE" -> "¡Proveedor registrado con éxito!"
                "SUPPLIER_UPDATE" -> "Datos actualizados correctamente"
                "SUPPLIER_DELETE" -> "Proveedor eliminado"
                else -> ""
            }
            ToastHelper.clasicCustomToast(binding.root, mensaje, true)
            binding.root.postDelayed({
                viewModel.resetOperationStatus()
            }, 100)
        }

        viewModel.isLoading.observe(this) { loading ->
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
                binding.rvSuppliers.isVisible = false
                binding.layoutMessage.isVisible = true
                binding.tvTitleMeassage.text = mensaje
                binding.btnRetry.isVisible = false
                binding.btnAddProveedor.isEnabled = true
                binding.btnSearch.isEnabled = true
            } else {
                //mostramos el recyclerView
                binding.rvSuppliers.isVisible = true
                binding.layoutMessage.isVisible = false
                binding.btnAddProveedor.isEnabled = true
                binding.btnSearch.isEnabled = true
            }
        } else {
            //mostramos mensaje ya sea sin datos o error
            binding.rvSuppliers.isVisible = false
            binding.layoutMessage.isVisible = true
            binding.tvTitleMeassage.text = mensaje
            binding.btnRetry.isVisible = true
            binding.btnAddProveedor.isEnabled = false
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
        binding.rvSuppliers.isVisible = !isEmpty
//        binding.tvEmptyState.isVisible = isEmpty
    }
    override fun onSupportNavigateUp(): Boolean {
        // Esto simula presionar el botón físico/virtual de "atrás" del teléfono
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}