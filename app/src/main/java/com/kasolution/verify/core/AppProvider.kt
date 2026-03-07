package com.kasolution.verify.core

import android.content.Context
import com.kasolution.verify.UI.Access.viewModel.LoginViewModelFactory
import com.kasolution.verify.UI.Category.viewModel.CategoriesViewModelFactory
import com.kasolution.verify.UI.Clients.viewModel.ClientesViewModelFactory
import com.kasolution.verify.UI.Dashboard.viewModel.DashboardViewModelFactory
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModelFactory
import com.kasolution.verify.UI.Inventory.viewModel.InventoryViewModelFactory
import com.kasolution.verify.UI.Sales.viewModel.SalesViewModelFactory
import com.kasolution.verify.UI.Suppliers.viewModel.SuppliersViewModelFactory
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.local.SettingsManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.repository.AuthRepository
import com.kasolution.verify.data.repository.CategoriesRepository
import com.kasolution.verify.data.repository.ClientsRepository
import com.kasolution.verify.data.repository.EmpleadoRepository
import com.kasolution.verify.data.repository.InventoryRepository
import com.kasolution.verify.data.repository.SalesRepository
import com.kasolution.verify.data.repository.SuppliersRepository
import com.kasolution.verify.domain.usecases.Categories.DeleteCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.GetCategoriesUseCase
import com.kasolution.verify.domain.usecases.Categories.SaveCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.UpdateCategoryUseCase
import com.kasolution.verify.domain.usecases.Clients.DeleteClientUseCase
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Clients.SaveClientUseCase
import com.kasolution.verify.domain.usecases.Clients.UpdateClientUseCase
import com.kasolution.verify.domain.usecases.Employees.DeleteEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.UpdateEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.GetEmpleadosUseCase
import com.kasolution.verify.domain.usecases.Employees.SaveEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Inventory.DeleteProductUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Inventory.SaveProductUseCase
import com.kasolution.verify.domain.usecases.Inventory.UpdateProductUseCase
import com.kasolution.verify.domain.usecases.Sales.DeleteSaleUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSaleDetailUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSalesHistoryUseCase
import com.kasolution.verify.domain.usecases.Sales.SaveSaleUseCase
import com.kasolution.verify.domain.usecases.Session.LogoutUseCase
import com.kasolution.verify.domain.usecases.Suppliers.DeleteSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase
import com.kasolution.verify.domain.usecases.Suppliers.SaveSupplierUseCase
import com.kasolution.verify.domain.usecases.Suppliers.UpdateSupplierUseCase

object AppProvider {

    private val socketManager by lazy {
        SocketManager.getInstance()
    }

    // --- INSTANCIAS ÚNICAS (SINGLETONS) PARA REPOSITORIOS ---
    private var authRepositoryInstance: AuthRepository? = null // <-- AGREGADO
    private var empleadoRepositoryInstance: EmpleadoRepository? = null
    private var clientsRepositoryInstance: ClientsRepository? = null
    private var suppliersRepositoryInstance: SuppliersRepository? = null
    private var inventoryRepositoryInstance: InventoryRepository? = null
    private var categoriesRepositoryInstance: CategoriesRepository? = null

    private var salesRepositoryInstance: SalesRepository? = null


    private var sessionManagerInstance: SessionManager? = null

    /**
     * Inicializa la conexión usando la IP guardada.
     */
    fun connectSocket(context: Context) {
        val settings = SettingsManager(context)
        val url = settings.getServerUrl()

        if (!socketManager.isConnected) {
            socketManager.connect(url)
        }
    }

    // --- SESIÓN ---

    fun provideSessionManager(context: Context): SessionManager {
        return sessionManagerInstance ?: synchronized(this) {
            sessionManagerInstance ?: SessionManager(context.applicationContext).also {
                sessionManagerInstance = it
            }
        }
    }

    // --- LOGIN (CORREGIDO) ---

    private fun getAuthRepository(context: Context): AuthRepository {
        val session = provideSessionManager(context)
        return authRepositoryInstance ?: synchronized(this) {
            authRepositoryInstance ?: AuthRepository(socketManager, session).also {
                authRepositoryInstance = it
            }
        }
    }

    fun provideLoginViewModelFactory(context: Context): LoginViewModelFactory {
        val authRepo = getAuthRepository(context)
        return LoginViewModelFactory(authRepo)
    }

    // --- EMPLEADOS ---

    private fun getEmpleadoRepository(): EmpleadoRepository {
        return empleadoRepositoryInstance ?: synchronized(this) {
            empleadoRepositoryInstance ?: EmpleadoRepository(socketManager).also {
                empleadoRepositoryInstance = it
            }
        }
    }

    fun provideEmpleadosViewModelFactory(): EmpleadosViewModelFactory {
        val repo = getEmpleadoRepository()

        return EmpleadosViewModelFactory(
            GetEmpleadosUseCase(repo),
            SaveEmpleadoUseCase(repo),
            UpdateEmpleadoUseCase(repo),
            DeleteEmpleadoUseCase(repo),
            socketManager
        )
    }

    // --- CLIENTES ---

    private fun getClientsRepository(): ClientsRepository {
        return clientsRepositoryInstance ?: synchronized(this) {
            clientsRepositoryInstance ?: ClientsRepository(socketManager).also {
                clientsRepositoryInstance = it
            }
        }
    }

    fun provideClientsViewModelFactory(): ClientesViewModelFactory {
        val repo = getClientsRepository()

        return ClientesViewModelFactory(
            GetClientsUseCase(repo),
            SaveClientUseCase(repo),
            UpdateClientUseCase(repo),
            DeleteClientUseCase(repo),
            socketManager
        )
    }
    // --- CATEGORIAS ---

    private fun getCategoriesRepository(): CategoriesRepository {
        return categoriesRepositoryInstance ?: synchronized(this) {
            categoriesRepositoryInstance ?: CategoriesRepository(socketManager).also {
                categoriesRepositoryInstance = it
            }
        }
    }

    fun provideCategoriesViewModelFactory(): CategoriesViewModelFactory {
        val repo = getCategoriesRepository()

        return CategoriesViewModelFactory(
            GetCategoriesUseCase(repo),
            SaveCategoryUseCase(repo),
            UpdateCategoryUseCase(repo),
            DeleteCategoryUseCase(repo),
            socketManager
        )
    }

    // --- PROVEEDORES ---

    private fun getSuppliersRepository(): SuppliersRepository {
        return suppliersRepositoryInstance ?: synchronized(this) {
            suppliersRepositoryInstance ?: SuppliersRepository(socketManager).also {
                suppliersRepositoryInstance = it
            }
        }
    }

    fun provideSuppliersViewModelFactory(): SuppliersViewModelFactory {
        val repo = getSuppliersRepository()

        return SuppliersViewModelFactory(
            GetSuppliersUseCase(repo),
            SaveSupplierUseCase(repo),
            UpdateSupplierUseCase(repo),
            DeleteSupplierUseCase(repo),
            socketManager
        )
    }
    // --- INVENTARIO ---

    private fun getInventoryRepository(): InventoryRepository {
        return inventoryRepositoryInstance ?: synchronized(this) {
            inventoryRepositoryInstance ?: InventoryRepository(socketManager).also {
                inventoryRepositoryInstance = it
            }
        }
    }

    fun provideInventoryViewModelFactory(): InventoryViewModelFactory {
        val repo = getInventoryRepository()
        val repoSupplier = getSuppliersRepository()
        val repoCategory = getCategoriesRepository()

        return InventoryViewModelFactory(
            GetProductsUseCase(repo),
            SaveProductUseCase(repo),
            UpdateProductUseCase(repo),
            DeleteProductUseCase(repo),
            GetSuppliersUseCase(repoSupplier),
            GetCategoriesUseCase(repoCategory),
            SaveCategoryUseCase(repoCategory),
            socketManager
        )
    }
    // --- VENTAS ---

    private fun getSalesRepository(): SalesRepository {
        return salesRepositoryInstance ?: synchronized(this) {
            salesRepositoryInstance ?: SalesRepository(socketManager).also {
                salesRepositoryInstance = it
            }
        }
    }

    fun provideSalesViewModelFactory(): SalesViewModelFactory {
        val repoInventory = getInventoryRepository()
        val repoClient = getClientsRepository()
        val repoSale = getSalesRepository()



        return SalesViewModelFactory(
            SaveSaleUseCase(repoSale),
            GetSalesHistoryUseCase(repoSale),
            DeleteSaleUseCase(repoSale),
            GetProductsUseCase(repoInventory),
            GetClientsUseCase(repoClient),
            GetSaleDetailUseCase(repoSale),
            socketManager
        )
    }


    // --- DASHBOARD ---

    fun provideDashboardViewModelFactory(context: Context): DashboardViewModelFactory {
        val sessionManager = provideSessionManager(context)

        // Incluimos authRepositoryInstance para que el Logout lo limpie
        val clearables = listOfNotNull(
            authRepositoryInstance,
            empleadoRepositoryInstance,
            clientsRepositoryInstance
        )

        val logoutUseCase = LogoutUseCase(
            sessionManager = sessionManager,
            socketManager = socketManager,
            onLogout = { clearAllInstances() }
        )

        return DashboardViewModelFactory(
            sessionManager = sessionManager,
            logoutUseCase = logoutUseCase
        )
    }

    /**
     * Función para resetear las instancias de los repositorios manualmente si es necesario.
     */
    fun clearAllInstances() {
        authRepositoryInstance?.clear()
        empleadoRepositoryInstance?.clear()
        clientsRepositoryInstance?.clear()

        authRepositoryInstance = null
        empleadoRepositoryInstance = null
        clientsRepositoryInstance = null
    }
}