package com.kasolution.verify.UI.Components.Scanner

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.repository.InventoryRepository
import com.kasolution.verify.domain.Inventory.model.Product
import java.util.UUID

class ScannerViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val TAG = "ScannerViewModel"

    private val _productFound = MutableLiveData<Product?>()
    private var currentRequestId: String? = null
    val productFound: LiveData<Product?> = _productFound

    private var lastSearchCode: String? = null
    private var sucursalActivaId: Int = 0

    init {
        // El registro del observer del socket se centraliza aquí para este componente
        repository.registerObserver()
        setupRepositoryObserver()
    }

    private fun setupRepositoryObserver() {
        // OJO: Al asignar esto, asumimos el control temporal del flujo de respuesta.
        // Es seguro siempre y cuando se limpie en el onCleared.
        repository.onProductsListReceived = { productos ->
            lastSearchCode?.let { code ->
                // Buscamos el producto por código de barras dentro del catálogo de la sucursal actual
                val p = productos.find { it.codigo == code }
                Log.d(TAG, "Búsqueda por código '$code' completada. Encontrado: ${p != null}")
                _productFound.postValue(p)
            }
        }

        // Manejo preventivo si la consulta de productos de la sucursal falla en el backend
        repository.onOperationResult = { action: String, exito: Boolean, errorMessage: String? ->
            if (action == "PRODUCT_GET_BY_CODE" && !exito) {
                val error = errorMessage ?: "Error desconocido"
                Log.e(TAG, "Error al escanear código '$lastSearchCode': $error")
                _productFound.postValue(null) // Notifica que no se encontró o no cumple las condiciones
                lastSearchCode = null // Limpiamos para el siguiente escaneo
            }
        }
    }
    fun findProductByCode(code: String, idSucursal: Int, modo:String) {
        if (idSucursal <= 0) {
            Log.e(TAG, "No se puede escanear código sin un contexto de sucursal válido.")
            _productFound.postValue(null)
            return
        }

        this.lastSearchCode = code
        this.sucursalActivaId = idSucursal

        if (code.isBlank()) {
            _productFound.postValue(null)
            return
        }
        currentRequestId = UUID.randomUUID().toString()
        repository.getProductByCode(code,idSucursal,modo,currentRequestId!!)
    }

    fun resetScanner() {
        _productFound.value = null
        lastSearchCode = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Destruyendo ScannerViewModel -> Rompiendo hooks para evitar fugas de memoria")
        repository.clear()
    }
}