package com.kasolution.verify.UI.Inventory.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.UI.Suppliers.model.Supplier
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
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Inventory)")
            getProductsUseCase.repository.onSocketReconnected()
        }

        if (socketManager.isConnected) {
            loadProducts()
            loadCategories()
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
            // Exponemos la lista a la View (Activity)
            _suppliersList.postValue(lista)
            _isLoading.postValue(false)
        }
        categoriesRepo.onCategoriesListReceived = { lista ->
            Log.d(TAG, "Cantidad de categorias recibidos: ${lista.size}")
            // Exponemos la lista a la View (Activity)
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
                    loadProducts()
                    //if (accion == "CATEGORY_SAVE") loadSuppliers()  //analizar bien si se utilizara en este contexto
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Inventory: $accion")
                        currentRequestId = null
                    }
                }
            }

        repo.onOperationResult = resultHandler
        supplierRepo.onOperationResult = resultHandler
        categoriesRepo.onOperationResult = resultHandler
        saveCategoryUseCase.repository.onOperationResult = resultHandler
        saveProductUseCase.repository.onOperationResult = resultHandler
        updateProductUseCase.repository.onOperationResult = resultHandler
        deleteProductUseCase.repository.onOperationResult = resultHandler
    }

    fun loadProducts() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getProductsUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }
    fun loadSuppliers(){
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getSuppliersUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }
    fun loadCategories(){
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getCategoriesUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }
    fun saveCategory(nombre: String, descripcion: String){
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val category = Category(
            id = 0,
            nombre = nombre,
            descripcion =descripcion
        )
        saveCategoryUseCase(category, currentRequestId!!)
    }

    fun saveProduct(
        codigo: String,
        nombre: String,
        idCategoria: Int,
        idProveedor: Int,
        precioCompra: Double,
        precioVenta: Double,
        stock: Int,
        unidadMedida: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val product = Product(
            id = 0,
            codigo = codigo,
            nombre = nombre,
            idCategoria = idCategoria,
            idProveedor = idProveedor,
            precioCompra = precioCompra,
            precioVenta = precioVenta,
            stock = stock,
            unidadMedida = unidadMedida,
            estado = estado
        )
        saveProductUseCase(product, currentRequestId!!)
    }

    fun updateProduct(
        id: Int,
        codigo: String,
        nombre: String,
        idCategoria: Int,
        idProveedor: Int,
        precioCompra: Double,
        precioVenta: Double,
        stock: Int,
        unidadMedida: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val product = Product(
            id = id, codigo = codigo,
            nombre = nombre,
            idCategoria = idCategoria,
            idProveedor = idProveedor,
            precioCompra = precioCompra,
            precioVenta = precioVenta,
            stock = stock,
            unidadMedida = unidadMedida,
            estado = estado
        )
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
        Log.d(TAG, "InventoryViewModel destruido")
    }
}
