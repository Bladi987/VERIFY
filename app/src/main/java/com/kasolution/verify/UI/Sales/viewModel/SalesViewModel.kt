package com.kasolution.verify.UI.Sales.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.UI.Sales.model.CartItem
import com.kasolution.verify.data.local.SessionManager
import com.kasolution.verify.domain.usecases.Clients.GetClientsUseCase
import com.kasolution.verify.domain.usecases.Inventory.GetProductsUseCase
import com.kasolution.verify.domain.usecases.Sales.DeleteSaleUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSalesHistoryUseCase
import com.kasolution.verify.domain.usecases.Sales.SaveSaleUseCase
import com.kasolution.verify.domain.usecases.Sales.GetSaleDetailUseCase
import java.util.UUID

class SalesViewModel(
    private val sesionManager: SessionManager,
    private val saveSaleUseCase: SaveSaleUseCase,
    private val getSalesHistoryUseCase: GetSalesHistoryUseCase,
    private val deleteSaleUseCase: DeleteSaleUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getClientsUseCase: GetClientsUseCase,
    private val getSaleDetailUseCase: GetSaleDetailUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "SalesViewModel"
    private var currentRequestId: String? = null
    val userId: Int = sesionManager.getUserId()
    val userName: String = sesionManager.getUserName()
    val userRole: String = sesionManager.getUserRole()
    private val gson = Gson()

    // --- LIVE DATA DEL CARRITO ---
    private val _cartList = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartList: LiveData<MutableList<CartItem>> get() = _cartList

    private val _totalVenta = MutableLiveData(0.0)
    val totalVenta: LiveData<Double> get() = _totalVenta

    // --- LIVE DATA DE DATOS EXTERNOS ---
    private val _productsList = MutableLiveData<List<Product>>()
    val productsList: LiveData<List<Product>> get() = _productsList

    private val _clientsList = MutableLiveData<List<Client>>()
    val clientsList: LiveData<List<Client>> get() = _clientsList

    private val _salesHistory = MutableLiveData<List<Map<String, Any>>>()
    val salesHistory: LiveData<List<Map<String, Any>>> get() = _salesHistory

    // --- NUEVO: LiveData para el Comprobante Completo (Backend Dinámico) ---
    private val _invoiceFullData = MutableLiveData<Map<String, Any>?>()
    val invoiceFullData: LiveData<Map<String, Any>?> get() = _invoiceFullData

    // --- LIVE DATA DE ESTADO ---
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        saveSaleUseCase.repository.registerObserver()
        getProductsUseCase.repository.registerObserver()
        getClientsUseCase.repository.registerObserver()

        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado -> Sincronizando datos de venta")
            loadAllData()
        }

        if (socketManager.isConnected) {
            loadAllData()
        }
    }

    private fun loadAllData() {
        loadSalesHistory()
        loadProducts()
        loadClientes()
    }

    private fun setupRepositoryObservers() {
        val salesRepo = saveSaleUseCase.repository
        val productRepo = getProductsUseCase.repository
        val clientRepo = getClientsUseCase.repository

        // Escuchar Historial de Ventas
        salesRepo.onSalesHistoryReceived = { lista ->
            _salesHistory.postValue(lista.reversed())
            _isLoading.postValue(false)
        }

        salesRepo.onInvoiceDataReceived = { jsonString ->
            try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val fullMap: Map<String, Any> = gson.fromJson(jsonString, type)
                _invoiceFullData.postValue(fullMap)
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando invoice data: ${e.message}")
            }
            _isLoading.postValue(false)
        }

        // Escuchar Productos
        productRepo.onProductsListReceived = { lista ->
            _productsList.postValue(lista)
            _isLoading.postValue(false)
        }

        // Escuchar Clientes
        clientRepo.onClientsListReceived = { lista ->
            _clientsList.postValue(lista)
            _isLoading.postValue(false)
        }

        // Manejador de resultados general
        val resultHandler: (String, Boolean, String?) -> Unit =
            { accion, exito, requestIdRecibido ->
                if (requestIdRecibido == currentRequestId || requestIdRecibido == null) {
                    _isLoading.postValue(false)
                    if (exito) {
                        if (accion == "SALE_SAVE") {
                            clearCart()
                        }
                        _operationSuccess.postValue(accion)
                        currentRequestId = null
                        // Recargamos historial para que la nueva venta aparezca en la lista de atrás
                        loadSalesHistory()
                    } else {
                        exception.postValue("Error en operación: $accion")
                        currentRequestId = null
                    }
                }
            }
        salesRepo.onOperationResult = resultHandler
    }

    // --- LÓGICA DE CARGA ---

    fun loadProducts() {
        if (socketManager.isConnected) getProductsUseCase()
    }

    fun loadClientes() {
        if (socketManager.isConnected) getClientsUseCase()
    }

    fun loadSalesHistory() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) getSalesHistoryUseCase()
        else _isLoading.postValue(false)
    }

    /**
     * Solicita al servidor el comprobante completo (Header, Items, Business)
     */
    fun loadSaleDetail(idVenta: Int) {
        _isLoading.postValue(true)
        // Limpiamos el anterior para que no parpadee el ticket viejo
        _invoiceFullData.value = null
        if (socketManager.isConnected) getSaleDetailUseCase(idVenta)
        else _isLoading.postValue(false)
    }

    // --- LÓGICA DE ANULACIÓN ---

    fun annulSale(idVenta: Int) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        currentRequestId = UUID.randomUUID().toString()

        if (socketManager.isConnected) {
            deleteSaleUseCase(idVenta, currentRequestId!!)
        } else {
            _isLoading.value = false
            exception.postValue("Sin conexión al servidor")
        }
    }

    // --- LÓGICA DEL CARRITO ---

    fun addProductToCart(product: Product) {
        val currentList = _cartList.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.producto.id == product.id }

        if (index != -1) {
            val item = currentList[index]
            if (item.cantidad < product.stock) {
                currentList[index] = item.copy(cantidad = item.cantidad + 1)
                _cartList.value = currentList
            } else {
                exception.postValue("Sin stock: Máximo ${product.stock}")
            }
        } else {
            if (product.stock > 0) {
                currentList.add(CartItem(producto = product, cantidad = 1))
                _cartList.value = currentList
            } else {
                exception.postValue("Producto agotado")
            }
        }
        calculateTotals()
    }

    fun updateQuantity(idProducto: Int, nuevaCantidad: Int) {
        val currentList = _cartList.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.producto.id == idProducto }

        if (index != -1) {
            val item = currentList[index]
            if (nuevaCantidad > 0 && nuevaCantidad <= item.producto.stock) {
                currentList[index] = item.copy(cantidad = nuevaCantidad)
                _cartList.value = currentList
                calculateTotals()
            }
        }
    }

    fun updatePrice(idProducto: Int, nuevoPrecio: Double) {
        val currentList = _cartList.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.producto.id == idProducto }
        if (index != -1 && nuevoPrecio >= 0) {
            currentList[index] =
                currentList[index].copy(producto = currentList[index].producto.copy(precioVenta = nuevoPrecio))
            _cartList.value = currentList
            calculateTotals()
        }
    }

    fun removeItem(idProducto: Int) {
        val currentList = _cartList.value?.toMutableList() ?: return
        currentList.removeAll { it.producto.id == idProducto }
        _cartList.value = currentList
        calculateTotals()
    }

    private fun calculateTotals() {
        val total = _cartList.value?.sumOf { it.subtotal } ?: 0.0
        _totalVenta.postValue(total)
    }

    fun clearCart() {
        _cartList.value = mutableListOf()
        _totalVenta.value = 0.0
    }

    fun clearInvoice() {
        _invoiceFullData.value = null
    }

    // --- PERSISTENCIA ---

    fun saveSale(idCliente: Int?, idEmpleado: Int, metodoPago: String, idTipoComprobante: Int) {
        if (_isLoading.value == true) return
        if (_cartList.value.isNullOrEmpty()) return

        _isLoading.value = true
        _invoiceFullData.value = null // Limpiar ticket previo
        currentRequestId = UUID.randomUUID().toString()

        val detalles = _cartList.value!!.map { item ->
            mapOf(
                "id_producto" to item.producto.id,
                "cantidad" to item.cantidad,
                "precio_unitario" to item.producto.precioVenta
            )
        }

        // Pasamos el Int al UseCase (asegúrate de que tu UseCase también acepte Int ahora)
        saveSaleUseCase(
            idCliente,
            idEmpleado,
            _totalVenta.value ?: 0.0,
            metodoPago,
            idTipoComprobante, // Enviamos el ID (1, 2 o 3)
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
        saveSaleUseCase.repository.clear()
        getProductsUseCase.repository.clear()
        getClientsUseCase.repository.clear()
    }
}