package com.kasolution.verify.UI.Employees.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Branch.GetBranchesUseCase
import com.kasolution.verify.domain.usecases.Employees.DeleteEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.UpdateEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Employees.GetEmpleadosUseCase
import com.kasolution.verify.domain.usecases.Employees.SaveEmpleadoUseCase
import com.kasolution.verify.domain.usecases.Roles.GetRoleUseCase

class EmpleadosViewModelFactory(
    private val getEmpleadosUseCase: GetEmpleadosUseCase,
    private val saveEmpleadoUseCase: SaveEmpleadoUseCase,
    private val updateEmpleadoUseCase: UpdateEmpleadoUseCase,
    private val deleteEmpleadoUseCase: DeleteEmpleadoUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getRolesUseCase: GetRoleUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmpleadosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmpleadosViewModel(
                getEmpleadosUseCase,
                saveEmpleadoUseCase,
                updateEmpleadoUseCase,
                deleteEmpleadoUseCase,
                getBranchesUseCase,
                getRolesUseCase,
                socketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}