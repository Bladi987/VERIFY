package com.kasolution.verify.UI.Purchase.history

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Purchase.adapter.PurchaseHistoryAdapter
import com.kasolution.verify.UI.Purchase.fragment.PurchaseDetailSheet
import com.kasolution.verify.UI.Purchase.viewModel.PurchaseViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityHistoryPurchaseBinding


class PurchaseHistoryActivity: AppCompatActivity()  {

    private val TAG = "PurchaseHistoryActivity"
    private lateinit var binding: ActivityHistoryPurchaseBinding
    private lateinit var historyAdapter: PurchaseHistoryAdapter
    private var fullHistoryList: List<Map<String, Any>> = emptyList()

    // Usamos el Factory de compras desde tu AppProvider
    private val viewModel: PurchaseViewModel by viewModels {
        AppProvider.providePurchaseViewModelFactory(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupSearchView()

    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarHistory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Mantiene la navegación hacia atrás consistente
        binding.toolbarHistory.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        // Inicializamos el adapter con el click para mostrar detalle
        historyAdapter = PurchaseHistoryAdapter(emptyList()) { compraSeleccionada ->
            val idCompra = compraSeleccionada["id_compra"].toString().toInt()
            mostrarDetalleCompra(idCompra)
        }

        binding.rvPurchaseHistory.apply {
            layoutManager = LinearLayoutManager(this@PurchaseHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupObservers() {
        // Observar la lista de compras desde el servidor
        viewModel.purchaseHistory.observe(this) { list ->
            fullHistoryList = list
            historyAdapter.updateList(list)
        }

        // Manejar el éxito de la anulación
        viewModel.operationSuccess.observe(this) { action ->
            if (action == "PURCHASE_DELETE") {
                ToastHelper.clasicCustomToast(binding.root, "Compra anulada con éxito", true)
                viewModel.loadPurchaseHistory() // Recargar lista para ver el cambio de estado
            }
        }

        // Manejar errores (ej: falta de conexión o error en DB)
        viewModel.exception.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSearchView() {
        binding.searchViewHistory.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterHistory(newText)
                return true
            }
        })
    }

    private fun filterHistory(query: String?) {
        if (query.isNullOrBlank()) {
            historyAdapter.updateList(fullHistoryList)
            return
        }

        val filtered = fullHistoryList.filter { purchase ->
            val id = purchase["id_compra"].toString()
            val proveedor = purchase["proveedor_nombre"]?.toString() ?: "Desconocido"
            // Filtra por ID de compra o por nombre del proveedor
            id.contains(query, ignoreCase = true) || proveedor.contains(query, ignoreCase = true)
        }
        historyAdapter.updateList(filtered)
    }
    fun confirmAnnul(idCompra: Int) {
        DialogHelper.showConfirmation(
            context = this,
            title = "Anular Compra",
            message = "¿Estás seguro de que deseas anular la compra #$idCompra?\n\nEsta acción restará los productos del stock y marcará el registro como ANULADO.",
            textPositiveButton = "Si, Anular",
            textNegativeButton = "Cancelar",
            onConfirm = {
                viewModel.annulPurchase(idCompra)
            }
        )
    }

    private fun mostrarDetalleCompra(idCompra: Int) {
        // Cargamos los datos en el ViewModel antes de mostrar el BottomSheet
        viewModel.loadPurchaseDetail(idCompra)

        // Creamos y mostramos el BottomSheet (o DialogFragment) de detalle
        val detailSheet = PurchaseDetailSheet.newInstance(isFromHistory = true)
        detailSheet.show(supportFragmentManager, "PurchaseDetailSheet")
    }
}