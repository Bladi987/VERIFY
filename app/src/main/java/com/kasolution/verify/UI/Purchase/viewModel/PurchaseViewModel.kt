package com.kasolution.verify.UI.Purchase.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.supplier.model.Supplier
import com.kasolution.verify.domain.purchase.SavePurchaseUseCase
import com.kasolution.verify.domain.purchase.GetPurchaseHistoryUseCase
import com.kasolution.verify.domain.purchase.GetPurchaseDetailUseCase
import com.kasolution.verify.domain.purchase.DeletePurchaseUseCase
import com.kasolution.verify.UI.Purchase.model.PurchaseItem
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Suppliers.GetSuppliersUseCase
import java.util.UUID

class PurchaseViewModel(
    private val sesionManager: SessionManager,
    private val savePurchaseUseCase: SavePurchaseUseCase,
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
    private val deletePurchaseUseCase: DeletePurchaseUseCase,
    private val getPurchaseDetailUseCase: GetPurchaseDetailUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "PurchaseViewModel"
    private var currentRequestId: String? = null

    val userId: Int = sesionManager.getUserId()
    val userName: String = sesionManager.getUserName()
    val userRole: String = sesionManager.getUserRolesForDisplay()
    val idSucursal: Int = sesionManager.getSucursalId()

    // NUEVO: Obtener la sesión de caja activa (Para egresos en EFECTIVO)
    private val activeCashSessionId: Int get() = sesionManager.getActiveCashSessionId()

    // --- LIVE DATA DEL CARRITO ---
    private val _cartList = MutableLiveData<MutableList<PurchaseItem>>(mutableListOf())
    val cartList: LiveData<MutableList<PurchaseItem>> get() = _cartList

    private val _totalCompra = MutableLiveData(0.0)
    val totalCompra: LiveData<Double> get() = _totalCompra

    // --- LIVE DATA DE DATOS EXTERNOS ---
    private val _productsList = MutableLiveData<List<Product>>()
    val productsList: LiveData<List<Product>> get() = _productsList

    private val _suppliersList = MutableLiveData<List<Supplier>>()
    val suppliersList: LiveData<List<Supplier>> get() = _suppliersList

    private val _purchaseHistory = MutableLiveData<List<Map<String, Any>>>()
    val purchaseHistory: LiveData<List<Map<String, Any>>> get() = _purchaseHistory

    private val _purchaseDetailData = MutableLiveData<Map<String, Any>?>()
    val purchaseDetailData: LiveData<Map<String, Any>?> get() = _purchaseDetailData

    // --- LIVE DATA DE ESTADO ---
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        savePurchaseUseCase.repository.registerObserver()
        getProductsUseCase.repository.registerObserver()
        getSuppliersUseCase.repository.registerObserver()
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado -> Sincronizando datos de compra")
            loadAllData()
        }

        if (socketManager.isConnected) {
            loadAllData()
        }
    }

    private fun loadAllData() {
        loadPurchaseHistory()
        loadProducts()
        loadSuppliers()
    }

    private fun setupRepositoryObservers() {
        val purchaseRepo = savePurchaseUseCase.repository
        val productRepo = getProductsUseCase.repository
        val supplierRepo = getSuppliersUseCase.repository

        purchaseRepo.onPurchaseHistoryReceived = { lista ->
            _purchaseHistory.postValue(lista.reversed())
            _isLoading.postValue(false)
        }

        purchaseRepo.onPurchaseDetailReceived = { data ->
            _purchaseDetailData.postValue(data)
            _isLoading.postValue(false)
        }

        productRepo.onProductsListReceived = { _productsList.postValue(it) }
        supplierRepo.onSuppliersListReceived = { _suppliersList.postValue(it) }

        purchaseRepo.onOperationResult = { accion, exito, resultMessage ->
            _isLoading.postValue(false)
            if (exito) {
                if (accion == "PURCHASE_SAVE") clearCart()
                _operationSuccess.postValue(accion)
                loadPurchaseHistory()
            } else {
                // Mensaje directo desde PHP (ej: "No hay saldo suficiente en caja")
                exception.postValue(resultMessage ?: "Error en la operación")
            }
            currentRequestId = null
        }
    }

    // --- ACCIONES DE CARGA ---

    fun loadPurchaseHistory() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) getPurchaseHistoryUseCase()
        else {
            _isLoading.postValue(false)
            exception.postValue("Sin conexión al servidor")
        }
    }

    fun loadPurchaseDetail(idCompra: Int) {
        _isLoading.postValue(true)
        if (socketManager.isConnected) getPurchaseDetailUseCase(idCompra)
        else _isLoading.postValue(false)
    }

    fun loadProducts() = if (socketManager.isConnected) getProductsUseCase(idSucursal,"PURCHASE") else Unit
    fun loadSuppliers() = if (socketManager.isConnected) getSuppliersUseCase() else Unit

    // --- LÓGICA DE ANULACIÓN ---

    fun annulPurchase(idCompra: Int) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        currentRequestId = UUID.randomUUID().toString()

        if (socketManager.isConnected) {
            deletePurchaseUseCase(idCompra, currentRequestId!!)
        } else {
            _isLoading.value = false
            exception.postValue("Sin conexión")
        }
    }

    // --- GESTIÓN DEL CARRITO (Add, Update, Remove, Calculate) ---
    // (Mantenida igual para conservar tu lógica de negocio)
    fun addProductToCart(product: Product) {
        val currentList = _cartList.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.idProducto == product.id }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(cantidad = item.cantidad + 1)
        } else {
            currentList.add(PurchaseItem(
                idProducto = product.id,
                nombre = product.nombre,
                cantidad = 1,
                precioCompra = product.precioCompra,
                stockActual = product.stock
            ))
        }
        _cartList.value = currentList
        calculateTotals()
    }

    fun updateQuantity(idProducto: Int, nuevaCantidad: Int) {
        val currentList = _cartList.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.idProducto == idProducto }
        if (index != -1 && nuevaCantidad > 0) {
            currentList[index] = currentList[index].copy(cantidad = nuevaCantidad)
            _cartList.value = currentList
            calculateTotals()
        }
    }

    fun updatePrice(idProducto: Int, nuevoPrecio: Double) {
        val currentList = _cartList.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.idProducto == idProducto }
        if (index != -1 && nuevoPrecio >= 0) {
            currentList[index] = currentList[index].copy(precioCompra = nuevoPrecio)
            _cartList.value = currentList
            calculateTotals()
        }
    }

    fun removeItem(position: Int) {
        val currentList = _cartList.value?.toMutableList() ?: return
        if (position in currentList.indices) {
            currentList.removeAt(position)
            _cartList.value = currentList
            calculateTotals()
        }
    }

    private fun calculateTotals() {
        val total = _cartList.value?.sumOf { it.cantidad * it.precioCompra } ?: 0.0
        _totalCompra.postValue(total)
    }

    fun clearCart() {
        _cartList.value = mutableListOf()
        _totalCompra.value = 0.0
    }

    // --- REGISTRO DE COMPRA (EL GRAN REAJUSTE) ---

    fun savePurchase(idProveedor: Int, idEmpleado: Int, metodoPago: String) {
        if (_isLoading.value == true) return
        if (_cartList.value.isNullOrEmpty()) return

        // Validación: Si es efectivo, DEBE haber una caja abierta
//        if (metodoPago == "EFECTIVO" && activeCashSessionId <= 0) {
//            exception.postValue("Error: No puedes comprar en efectivo sin una caja abierta.")
//            return
//        }

        _isLoading.value = true
        currentRequestId = UUID.randomUUID().toString()

        val detalles = _cartList.value!!.map { item ->
            mapOf(
                "id_producto" to item.idProducto,
                "cantidad" to item.cantidad,
                "precio_compra" to item.precioCompra
            )
        }

        // REAJUSTE: Inyectamos idSesion y metodoPago
        savePurchaseUseCase(
            idProveedor,
            idEmpleado,
            idSucursal,
            activeCashSessionId,
            metodoPago,
            _totalCompra.value ?: 0.0,
            detalles,
            currentRequestId!!
        )
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        savePurchaseUseCase.repository.clear()
        getProductsUseCase.repository.clear()
        getSuppliersUseCase.repository.clear()
    }
}