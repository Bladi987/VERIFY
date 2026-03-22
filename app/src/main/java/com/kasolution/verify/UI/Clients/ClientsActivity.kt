package com.kasolution.verify.UI.Clients

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
import com.kasolution.verify.UI.Clientes.viewModel.ClientesViewModel
import com.kasolution.verify.UI.Clients.adapter.ClientesAdapter
import com.kasolution.verify.UI.Clients.fragment.ClientFormDialogFragment
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.BottomSheetHelper
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityClientsBinding
import kotlin.getValue

class ClientsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientsBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: ClientesAdapter
    private lateinit var lista: ArrayList<Client>
    private var selectedClient: Client? = null
    private val viewModel: ClientesViewModel by viewModels {
        AppProvider.provideClientsViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityClientsBinding.inflate(layoutInflater)
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
        binding.btnAddClient.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = ClientFormDialogFragment()
            // El "ClienteTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "ClientTag")
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadClientes()
        }

        binding.btnSearch.setOnClickListener {
            alternarTitulo()
        }
    }

    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = ClientesAdapter(
            listaInicial = lista,
            onClickListener = { cliente -> onItemClicListener(cliente) },
            onLongClickListener = { cliente, position -> showOptionsFor(cliente, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvClients.layoutManager = lmanager
        binding.rvClients.adapter = adapter

        adapter.onDataChanged(lista.isEmpty())
    }
    private fun onItemClicListener(cliente: Client) {
        //Toast.makeText(this, "${cliente.nombre} - ${cliente.usuario}", Toast.LENGTH_SHORT).show()
    }

    private fun showOptionsFor(cliente: Client, position: Int) {
        binding.etSearch.clearFocus()
        selectedClient = cliente
        adapter.setSelectedItem(position)
        BottomSheetHelper.showInventoryOptions(
            activity = this,
            cabeceraName = "Cliente",
            name = cliente.nombre,
            onEdit = {
                val dialog = ClientFormDialogFragment.newInstance(cliente)
                dialog.show(supportFragmentManager, "EditClient")
            },
            onDelete = {
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Cuenta",
                    "¿Estás seguro de que deseas eliminar a ${cliente.nombre}?",
                    onConfirm = {
                        viewModel.deleteCliente(cliente.id)
                    })
            },
            onDismiss = {
                // Limpieza visual cuando el menú se va
                selectedClient = null
                adapter.clearSelection()
            }
        )

    }
    private fun setupObservers() {
        viewModel.clientesList.observe(this) { lista ->
            val emptyList = lista.isNullOrEmpty()
            mostrarMensajeOrLista(true, emptyList)
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.exception.observe(this) { error ->
            mostrarMensajeOrLista(exito = false, mensaje = error)
            ToastHelper.showCustomToast(binding.root, error, false)
        }
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "CLIENTE_SAVE" -> "¡Cliente registrado con éxito!"
                "CLIENTE_UPDATE" -> "Datos actualizados correctamente"
                "CLIENTE_DELETE" -> "Cliente eliminado"
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
                binding.rvClients.isVisible = false
                binding.layoutMessage.isVisible = true
                binding.tvTitleMeassage.text = mensaje
                binding.btnRetry.isVisible = false
                binding.btnAddClient.isEnabled = true
                binding.btnSearch.isEnabled = true
            } else {
                //mostramos el recyclerView
                binding.rvClients.isVisible = true
                binding.layoutMessage.isVisible = false
                binding.btnAddClient.isEnabled = true
                binding.btnSearch.isEnabled = true
            }
        } else {
            //mostramos mensaje ya sea sin datos o error
            binding.rvClients.isVisible = false
            binding.layoutMessage.isVisible = true
            binding.tvTitleMeassage.text = mensaje
            binding.btnRetry.isVisible = true
            binding.btnAddClient.isEnabled = false
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
        binding.rvClients.isVisible = !isEmpty
//        binding.tvEmptyState.isVisible = isEmpty
    }
    override fun onSupportNavigateUp(): Boolean {
        // Esto simula presionar el botón físico/virtual de "atrás" del teléfono
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}