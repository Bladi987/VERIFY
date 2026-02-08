package com.kasolution.verify.UI.Clients

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kasolution.verify.UI.Clientes.viewModel.ClientesViewModel
import com.kasolution.verify.UI.Clients.adapter.ClientesAdapter
import com.kasolution.verify.UI.Clients.fragment.ClientFormDialogFragment
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.databinding.ActivityClientsBinding
import kotlin.getValue

class ClientsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientsBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: ClientesAdapter
    private lateinit var lista: ArrayList<Cliente>
    private var selectedClient: Cliente? = null
    private val viewModel: ClientesViewModel by viewModels {
        AppProvider.provideClientsViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityClientsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        lista = ArrayList()
        initRecycler()
        setupObservers()
        viewModel.loadClientes()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ejecutar el filtro cada vez que el texto cambie
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        binding.fabAddClient.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = ClientFormDialogFragment()
            // El "ClienteTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "ClientTag")
        }

        binding.btnEditOption.setOnClickListener {
            selectedClient?.let { emp ->
                // Abrimos el diálogo enviando el objeto cliente
                val dialog = ClientFormDialogFragment.newInstance(emp)
                dialog.show(supportFragmentManager, "EditClient")
                hideOptions()
            }
        }

        binding.btnDeleteOption.setOnClickListener {
            selectedClient?.let { emp ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Cuenta")
                    .setMessage("¿Estás seguro de que deseas eliminar a ${emp.nombre}?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deleteCliente(emp.id)
                        hideOptions()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        binding.btnCloseOptions.setOnClickListener {
            hideOptions()
        }
    }
    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = ClientesAdapter(
            listaInicial = lista,
            onClickListener = { cliente -> onItemClicListener(cliente) },
            onLongClickListener = { cliente,position -> showOptionsFor(cliente, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvClients.layoutManager = lmanager
        binding.rvClients.adapter = adapter

        adapter.onDataChanged(lista.isEmpty())
    }

    private fun onItemClicListener(cliente: Cliente) {
        //Toast.makeText(this, "${cliente.nombre} - ${cliente.usuario}", Toast.LENGTH_SHORT).show()
        hideOptions()
    }
    private fun showOptionsFor(cliente: Cliente,position: Int) {
        selectedClient = cliente
        binding.tvSelectedName.text = cliente.nombre
        adapter.setSelectedItem(position)

        if (binding.layoutOptions.visibility == View.GONE) {
            // 1. Lo hacemos "invisible" pero que ocupe espacio para que Android calcule su altura
            binding.layoutOptions.alpha = 0f
            binding.layoutOptions.visibility = View.VISIBLE

            binding.layoutOptions.post {
                val height = binding.layoutOptions.height.toFloat()

                // 2. Lo movemos abajo y le devolvemos la opacidad ANTES de que el usuario lo vea
                binding.layoutOptions.translationY = height
                binding.layoutOptions.alpha = 1f

                // 3. Ejecutamos la animación limpia
                binding.layoutOptions.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        } else {
            // Si ya es visible (cambio de selección), solo actualizamos el nombre con una pequeña vibración
            binding.tvSelectedName.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).withEndAction {
                binding.tvSelectedName.animate().scaleX(1f).scaleY(1f).start()
            }.start()
        }
    }

    private fun hideOptions() {
        adapter.clearSelection()
        binding.layoutOptions.animate()
            .translationY(binding.layoutOptions.height.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.layoutOptions.visibility = View.GONE
                selectedClient = null
            }
            .start()
    }

    private fun setupObservers() {
        viewModel.clientesList.observe(this) { lista ->
            // El Observer solo entrega la data.
            // El adaptador internamente dirá "estoy vacío" o "tengo datos"
            Log.d("ClientsActivity", lista.toString())
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.exception.observe(this) { error ->
            // Manejar el error según tus necesidades
            Snackbar.make(binding.root, error, Snackbar.LENGTH_INDEFINITE)
                .setAction("CERRAR") { } // Botón para descartar
                .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_light))
                .show()
        }
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "CLIENTE_SAVE" -> "¡Cliente registrado con éxito!"
                "CLIENTE_UPDATE" -> "Datos actualizados correctamente"
                "CLIENTE_DELETE" -> "Cliente eliminado"
                else -> ""
            }

            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_green_light))
                .show()
            binding.root.postDelayed({
                viewModel.resetOperationStatus()
            }, 100)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.isVisible = loading
        }
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        binding.rvClients.isVisible = !isEmpty
        binding.tvEmptyState.isVisible = isEmpty
    }
}