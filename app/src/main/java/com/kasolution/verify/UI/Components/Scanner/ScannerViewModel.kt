package com.kasolution.verify.UI.Components.Scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasolution.verify.data.repository.InventoryRepository
import com.kasolution.verify.domain.Inventory.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScannerViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val _productFound = MutableLiveData<Product?>()
    val productFound: LiveData<Product?> = _productFound

    init {
        // Configuramos el repositorio para que cuando llegue la lista (o el producto)
        // el ViewModel reaccione.
        repository.onInventoryListReceived = { productos ->
            // Buscamos el código que activó la búsqueda
            // Nota: En una versión pro, podrías pedir un PRODUCT_GET_BY_CODE al server
            // pero, si ya tienes la lista en el repo, buscamos aquí.
            lastSearchCode?.let { code ->
                val p = productos.find { it.codigo == code }
                _productFound.postValue(p)
            }
        }
    }

    private var lastSearchCode: String? = null

    fun findProductByCode(code: String) {
        lastSearchCode = code
        // Pedimos al servidor que actualice o simplemente usamos la lista local si el repo la tiene
        repository.getProducts()
    }
}