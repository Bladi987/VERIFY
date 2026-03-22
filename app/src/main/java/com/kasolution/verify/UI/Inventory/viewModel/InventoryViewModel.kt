package com.kasolution.verify.UI.Inventory.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.usecases.Categories.GetCategoriesUseCase
import com.kasolution.verify.domain.usecases.Categories.SaveCategoryUseCase
import com.kasolution.verify.domain.usecases.Inventory.DeleteProductUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Inventory.SaveProductUseCase
import com.kasolution.verify.domain.usecases.Inventory.UpdateProductUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase
import java.util.UUID

class InventoryViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "InventoryViewModel"
    private var currentRequestId: String? = null

    private val _productsList = MutableLiveData<List<Product>>()
    val productsList: LiveData<List<Product>> get() = _productsList

    private val _suppliersList = MutableLiveData<List<Supplier>>()
    val suppliersList: LiveData<List<Supplier>> get() = _suppliersList

    private val _categoriesList = MutableLiveData<List<Category>>()
    val categoryList: LiveData<List<Category>> get() = _categoriesList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        // Registro de observadores (Idéntico a Clientes)
        getProductsUseCase.repository.registerObserver()
        getSuppliersUseCase.repository.registerObserver()
        getCategoriesUseCase.repository.registerObserver()

        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket reconectado -> Sincronizando inventario")
            loadInitialData()
        }

        if (socketManager.isConnected) {
            loadInitialData()
        }
    }

    private fun setupRepositoryObservers() {
        val prodRepo = getProductsUseCase.repository
        val suppRepo = getSuppliersUseCase.repository
        val catRepo = getCategoriesUseCase.repository

        // Callbacks de Listas - NOTA: Nombres alineados con el Repository
        prodRepo.onProductsListReceived = { lista ->
            _productsList.postValue(lista)
            _isLoading.postValue(false) // Solo el flujo de productos apaga el loading principal
        }

        suppRepo.onSuppliersListReceived = { lista ->
            _suppliersList.postValue(lista)
        }

        catRepo.onCategoriesListReceived = { lista ->
            _categoriesList.postValue(lista)
        }

        // Handler de Resultados (Refactorizado para ser 100% como Clientes)
        val resultHandler: (String, Boolean, String?) -> Unit = { accion, exito, requestIdRecibido ->
            // Regla de oro: Cualquier respuesta de servidor detiene el progreso
            _isLoading.postValue(false)

            if (exito) {
                if (requestIdRecibido == currentRequestId) {
                    _operationSuccess.postValue(accion)
                    currentRequestId = null
                }

                // Refresco inteligente según la acción
                when(accion) {
                    "PRODUCT_SAVE", "PRODUCT_UPDATE", "PRODUCT_DELETE" -> loadProducts()
                    "CATEGORY_SAVE" -> loadCategories()
                }
            } else {
                if (requestIdRecibido == currentRequestId) {
                    exception.postValue("Error en servidor: $accion")
                    currentRequestId = null
                }
            }
        }

        // Asignación de handler único a todos los repositorios involucrados
        prodRepo.onOperationResult = resultHandler
        suppRepo.onOperationResult = resultHandler
        catRepo.onOperationResult = resultHandler
    }

    /* --- MÉTODOS DE CARGA --- */

    fun loadInitialData() {
        // Cargamos auxiliares primero y productos al final para que el loading sea coherente
        loadCategories()
        loadSuppliers()
        loadProducts()
    }

    fun loadProducts() {
        if (_isLoading.value == true) return
        _isLoading.postValue(true)
        if (socketManager.isConnected) getProductsUseCase()
        else exception.postValue("Servidor desconectado")
    }

    fun loadSuppliers() = getSuppliersUseCase()
    fun loadCategories() = getCategoriesUseCase()

    /* --- OPERACIONES --- */

    fun saveProduct(product: Product) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        saveProductUseCase(product, currentRequestId!!)
    }

    fun updateProduct(product: Product) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        updateProductUseCase(product, currentRequestId!!)
    }

    fun deleteProduct(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteProductUseCase(id, currentRequestId!!)
    }

    fun saveCategory(nombre: String, descripcion: String) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val category = Category(0, nombre, descripcion, true)
        saveCategoryUseCase(category, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        getProductsUseCase.repository.clear()
        getSuppliersUseCase.repository.clear()
        getCategoriesUseCase.repository.clear()
    }
}