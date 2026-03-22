package com.kasolution.verify.UI.Sales.History

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Sales.fragment.SaleDetailSheet
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.databinding.ActivityHistoryBinding
import com.kasolution.verify.UI.Sales.adapter.SalesHistoryAdapter
import com.kasolution.verify.core.utils.ToastHelper

class HistoryActivity : AppCompatActivity() {
    private val TAG = "HistoryActivity"
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: SalesHistoryAdapter
    private var fullHistoryList: List<Map<String, Any>> = emptyList()
    private var currentTicketSheet: SaleDetailSheet? = null

    // El ViewModel se comparte con el BottomSheet mediante activityViewModels()
    private val viewModel: SalesViewModel by viewModels {
        AppProvider.provideSalesViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupSearchView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarHistory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarHistory.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        historyAdapter = SalesHistoryAdapter(emptyList()) { saleSeleccionada ->
            val idVenta = saleSeleccionada["id_venta"].toString().toInt()
            mostrarDetalleVenta(idVenta)
        }

        binding.rvSalesHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupObservers() {
        viewModel.salesHistory.observe(this) { list ->
            fullHistoryList = list
            historyAdapter.updateList(list)
        }

        viewModel.operationSuccess.observe(this) { action ->
            if (action == "SALE_DELETE") {
                ToastHelper.showCustomToast(binding.root, "Venta anulada con éxito",true)
                viewModel.loadSalesHistory()
            }
        }

        // Opcional: Observar excepciones para mostrar errores de red o base de datos
        viewModel.exception.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
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

        val filtered = fullHistoryList.filter { sale ->
            val id = sale["id_venta"].toString()
            val cliente = sale["cliente_nombre"]?.toString() ?: "Público General"
            id.contains(query, ignoreCase = true) || cliente.contains(query, ignoreCase = true)
        }
        historyAdapter.updateList(filtered)
    }

    /**
     * Esta función es llamada desde el BottomSheet o el Adapter
     */
    fun confirmAnnul(idVenta: Int) {
        AlertDialog.Builder(this)
            .setTitle("Anular Venta")
            .setMessage("¿Estás seguro de que deseas anular la venta #$idVenta?\n\nEsta acción devolverá los productos al stock y marcará el comprobante como ANULADO.")
            .setPositiveButton("SÍ, ANULAR") { _, _ ->
                viewModel.annulSale(idVenta)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun mostrarDetalleVenta(idVenta: Int) {
        viewModel.loadSaleDetail(idVenta)
        currentTicketSheet = SaleDetailSheet.newInstance(isFromHistory = true)
        val ticketSheet = SaleDetailSheet.newInstance(isFromHistory = true)
        ticketSheet.show(supportFragmentManager, "SaleDetailSheet")
    }
}