package com.kasolution.verify.UI.Components.Scanner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasolution.verify.UI.Components.Scanner.ScannerViewModel
import com.kasolution.verify.data.repository.InventoryRepository

class ScannerViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScannerViewModel(repository) as T
    }
}