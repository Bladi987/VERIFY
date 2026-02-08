package com.kasolution.verify.core

import android.content.Context
import com.kasolution.verify.UI.Access.viewModel.LoginViewModelFactory
import com.kasolution.verify.UI.Clients.viewModel.ClientesViewModelFactory
import com.kasolution.verify.UI.Dashboard.viewModel.DashboardViewModelFactory
import com.kasolution.verify.UI.Employees.viewModel.EmpleadosViewModelFactory
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.data.local.SettingsManager
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.repository.AuthRepository
import com.kasolution.verify.data.repository.ClientsRepository
import com.kasolution.verify.data.repository.EmpleadoRepository
import com.kasolution.verify.domain.usecases.Clients.DeleteClientUseCase
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Clients.SaveClientUseCase
import com.kasolution.verify.domain.usecases.Clients.UpdateClientUseCase
import com.kasolution.verify.domain.usecases.Employees.DeleteEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.UpdateEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.GetEmpleadosUseCase
import com.kasolution.verify.domain.usecases.Employees.SaveEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Session.LogoutUseCase

object AppProvider {

    private val socketManager by lazy {
        SocketManager.getInstance()
    }

    // --- INSTANCIAS ÚNICAS (SINGLETONS) PARA REPOSITORIOS ---
    private var authRepositoryInstance: AuthRepository? = null // <-- AGREGADO
    private var empleadoRepositoryInstance: EmpleadoRepository? = null
    private var clientsRepositoryInstance: ClientsRepository? = null
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

        return DashboardViewModelFactory(sessionManager = sessionManager, logoutUseCase = logoutUseCase)
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