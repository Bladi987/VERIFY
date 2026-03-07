package com.kasolution.verify.UI.Inventory.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.data.network.SocketManager
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
        // 1. REACTIVACIÓN DE TODOS LOS REPOSITORIOS INVOLUCRADOS
        getProductsUseCase.repository.registerObserver()
        getSuppliersUseCase.repository.registerObserver()
        getCategoriesUseCase.repository.registerObserver()

        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Inventory) -> Recargando todo")
            loadProducts()
            loadCategories()
            loadSuppliers()
        }

        if (socketManager.isConnected) {
            loadProducts()
            loadCategories()
            loadSuppliers() // Añadido para consistencia inicial
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getProductsUseCase.repository
        val supplierRepo = getSuppliersUseCase.repository
        val categoriesRepo = getCategoriesUseCase.repository

        repo.onInventoryListReceived = { lista ->
            Log.d(TAG, "Cantidad de Productos recibidos: ${lista.size}")
            _productsList.postValue(lista)
            _isLoading.postValue(false)
        }

        supplierRepo.onSuppliersListReceived = { lista ->
            Log.d(TAG, "Cantidad de Proveedores recibidos: ${lista.size}")
            _suppliersList.postValue(lista)
            _isLoading.postValue(false)
        }

        categoriesRepo.onCategoriesListReceived = { lista ->
            Log.d(TAG, "Cantidad de categorias recibidos: ${lista.size}")
            _categoriesList.postValue(lista)
            _isLoading.postValue(false)
        }

        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->
                _isLoading.postValue(false)

                if (exito) {
                    if (requestIdRecibido == currentRequestId) {
                        _operationSuccess.postValue(accion)
                        currentRequestId = null
                    }
                    // Dependiendo de qué se guardó, refrescamos la lista correspondiente
                    when(accion) {
                        "PRODUCT_SAVE", "PRODUCT_UPDATE", "PRODUCT_DELETE" -> loadProducts()
                        "CATEGORY_SAVE" -> loadCategories()
                    }
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Inventory: $accion")
                        currentRequestId = null
                    }
                }
            }

        // Suscribimos el handler a todos los repositorios para capturar éxitos/errores
        repo.onOperationResult = resultHandler
        supplierRepo.onOperationResult = resultHandler
        categoriesRepo.onOperationResult = resultHandler
        saveCategoryUseCase.repository.onOperationResult = resultHandler
        saveProductUseCase.repository.onOperationResult = resultHandler
        updateProductUseCase.repository.onOperationResult = resultHandler
        deleteProductUseCase.repository.onOperationResult = resultHandler
    }

    /* --- MÉTODOS DE CARGA --- */

    fun loadProducts() {
        if (socketManager.isConnected) {
            _isLoading.postValue(true)
            getProductsUseCase()
        } else {
            exception.postValue("Servidor desconectado")
        }
    }

    fun loadSuppliers(){
        if (socketManager.isConnected) {
            _isLoading.postValue(true)
            getSuppliersUseCase()
        }
    }

    fun loadCategories(){
        if (socketManager.isConnected) {
            _isLoading.postValue(true)
            getCategoriesUseCase()
        }
    }

    /* --- OPERACIONES --- */

    fun saveCategory(nombre: String, descripcion: String){
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val category = Category(id = 0, nombre = nombre, descripcion = descripcion, estado = true)
        saveCategoryUseCase(category, currentRequestId!!)
    }

    fun saveProduct(codigo: String, nombre: String, idCategoria: Int, idProveedor: Int,
                    precioCompra: Double, precioVenta: Double, stock: Int, unidadMedida: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val product = Product(0, codigo, nombre, idCategoria, null, idProveedor, null,
            precioCompra, precioVenta, stock, unidadMedida, estado)
        saveProductUseCase(product, currentRequestId!!)
    }

    fun updateProduct(id: Int, codigo: String, nombre: String, idCategoria: Int, idProveedor: Int,
                      precioCompra: Double, precioVenta: Double, stock: Int, unidadMedida: String, estado: Boolean) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val product = Product(id, codigo, nombre, idCategoria, null, idProveedor, null,
            precioCompra, precioVenta, stock, unidadMedida, estado)
        updateProductUseCase(product, currentRequestId!!)
    }

    fun deleteProduct(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteProductUseCase(id, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "InventoryViewModel destruido - Limpiando 3 repositorios")
        // 2. LIMPIEZA ABSOLUTA DE LOS 3 OBSERVADORES
        getProductsUseCase.repository.clear()
        getSuppliersUseCase.repository.clear()
        getCategoriesUseCase.repository.clear()
    }
}