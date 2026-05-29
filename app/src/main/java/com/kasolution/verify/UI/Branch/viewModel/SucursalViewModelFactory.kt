package com.kasolution.verify.UI.Branch.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.UI.branch.viewModel.SucursalViewModel
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Branch.DeleteBranchUseCase
import com.kasolution.verify.domain.usecases.Branch.GetBranchesUseCase
import com.kasolution.verify.domain.usecases.Branch.SaveBranchUseCase
import com.kasolution.verify.domain.usecases.Branch.UpdateBranchUseCase

class SucursalViewModelFactory(
    private val getBranchesUseCase: GetBranchesUseCase,
    private val saveBranchUseCase: SaveBranchUseCase,
    private val updateBranchUseCase: UpdateBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SucursalViewModel(
            getBranchesUseCase,
            saveBranchUseCase,
            updateBranchUseCase,
            deleteBranchUseCase,
            socketManager
        ) as T
    }
}