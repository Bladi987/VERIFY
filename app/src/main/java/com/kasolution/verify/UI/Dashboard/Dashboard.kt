package com.kasolution.verify.UI.Dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Access.LoginActivity
import com.kasolution.verify.UI.Category.CategoriesActivity
import com.kasolution.verify.UI.Clients.ClientsActivity
import com.kasolution.verify.UI.Dashboard.View.adapter.gridMenuAdapter
import com.kasolution.verify.UI.Dashboard.View.model.itemGridMenu
import com.kasolution.verify.UI.Dashboard.viewModel.DashboardViewModel
import com.kasolution.verify.UI.Employees.EmployeesActivity
import com.kasolution.verify.UI.Inventory.InventoryActivity
import com.kasolution.verify.UI.Purchase.currentPurchase.PurchaseActivity
import com.kasolution.verify.UI.Purchase.history.PurchaseHistoryActivity
import com.kasolution.verify.UI.Sales.CurrentSale.SalesActivity
import com.kasolution.verify.UI.Sales.History.HistoryActivity
import com.kasolution.verify.UI.Settings.SettingsActivity
import com.kasolution.verify.UI.Suppliers.SuppliersActivity
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.databinding.ActivityDashboardBinding
import kotlin.jvm.java

class Dashboard : AppCompatActivity() {
    private lateinit var glmanager: GridLayoutManager
    private lateinit var adapterlistMenu: gridMenuAdapter
    private lateinit var Lista: ArrayList<itemGridMenu>
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Lista = llenarDatosMenu()
        init()
        val factory = AppProvider.provideDashboardViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        binding.tvUserName.text = viewModel.userName
        binding.tvUserRole.text = viewModel.userRole

        // Ejemplo de logout
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
        viewModel.logoutCompleted.observe(this) { logoutCompleted ->
            if (logoutCompleted) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        // Lógica para las Métricas y Alertas (opcional si ya están en XML)


    }

    private fun llenarDatosMenu(): ArrayList<itemGridMenu> {
        val arrayList: ArrayList<itemGridMenu> = ArrayList()
        arrayList.clear()
        arrayList.add(itemGridMenu(R.drawable.ic_module_ventas, "Ventas"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_compras, "Compras"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_inventario, "Inventario"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_clientes, "Clientes"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_proveedores, "Proveedores"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_history_sales, "Historial de Ventas"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_history_sales, "Historial de Compras"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_reportes, "Reportes"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_caja, "Caja"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_usuarios, "Usuarios"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_categoria, "Categorias"))
        arrayList.add(itemGridMenu(R.drawable.ic_module_ajustes, "Configuración"))
        return arrayList
    }

    private fun init() {
        if (Lista.isNotEmpty()) {
            val columnWidthDp = 300
            val columns = resources.displayMetrics.widthPixels / columnWidthDp
            glmanager = GridLayoutManager(this, columns)
            adapterlistMenu = gridMenuAdapter(
                listaRecibida = Lista,
                OnClickListener = { itemGridMenu -> onItemSelected(itemGridMenu) })
            binding.rvMenu.layoutManager = glmanager
            binding.rvMenu.adapter = adapterlistMenu
        } else
            binding.tvModulesTitle.text = "No hay opciones disponibles"
    }

    private fun onItemSelected(itemGridMenu: itemGridMenu) {
        when (itemGridMenu.name) {
            "Ventas" -> {
                // Lógica para la opción de Ventas
                val intent = Intent(this, SalesActivity::class.java)
                startActivity(intent)
            }
            "Compras" -> {
                val intent = Intent(this, PurchaseActivity::class.java)
                startActivity(intent)
            }

            "Inventario" -> {
                // Lógica para la opción de Inventario
                val intent = Intent(this, InventoryActivity::class.java)
                startActivity(intent)
            }

            "Clientes" -> {
                // Lógica para la opción de Clientes
                val intent = Intent(this, ClientsActivity::class.java)
                startActivity(intent)
            }

            "Proveedores" -> {
                // Lógica para la opción de Proveedores
                val intent = Intent(this, SuppliersActivity::class.java)
                startActivity(intent)
            }
            "Historial de Ventas" -> {
                // Lógica para la opción de Historial de Ventas
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }
            "Historial de Compras" -> {
                // Lógica para la opción de Historial de Ventas
                val intent = Intent(this, PurchaseHistoryActivity::class.java)
                startActivity(intent)
            }

            "Reportes" -> {
                // Lógica para la opción de Reportes
            }

            "Caja" -> {
                // Lógica para la opción de Caja
            }

            "Usuarios" -> {
                val intent = Intent(this, EmployeesActivity::class.java)
                startActivity(intent)
            }
            "Categorias" -> {
                val intent = Intent(this, CategoriesActivity::class.java)
                startActivity(intent)
            }

            "Configuración" -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

}


