package com.kasolution.verify.UI.Role.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Roles.DeleteRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.GetRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.SaveRoleUseCase
import com.kasolution.verify.domain.usecases.Roles.UpdateRoleUseCase

class RoleViewModelFactory(
    private val getRolesUseCase: GetRoleUseCase,
    private val saveRoleUseCase: SaveRoleUseCase,
    private val updateRoleUseCase: UpdateRoleUseCase,
    private val deleteRoleUseCase: DeleteRoleUseCase,
    private val socketManager: SocketManager
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        @Suppress("UNCHECKED_CAST")
        return RoleViewModel(
            getRolesUseCase,
            saveRoleUseCase,
            updateRoleUseCase,
            deleteRoleUseCase,
            socketManager
        ) as T
    }
}