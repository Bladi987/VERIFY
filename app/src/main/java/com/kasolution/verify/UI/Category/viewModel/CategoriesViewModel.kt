package com.kasolution.verify.UI.Category.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.domain.usecases.Categories.DeleteCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.GetCategoriesUseCase
import com.kasolution.verify.domain.usecases.Categories.SaveCategoryUseCase
import com.kasolution.verify.domain.usecases.Categories.UpdateCategoryUseCase
import java.util.UUID

class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = "CategoriesViewModel"
    private var currentRequestId: String? = null

    private val _categoriesList = MutableLiveData<List<Category>>()
    val categoriesList: LiveData<List<Category>> get() = _categoriesList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val exception = MutableLiveData<String>()

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> get() = _operationSuccess

    init {
        setupRepositoryObservers()

        socketManager.onConnected = {
            Log.d(TAG, "Socket conectado (Categoria)")
            getCategoriesUseCase.repository.onSocketReconnected()
        }

        if (socketManager.isConnected) {
            loadCategories()
        }
    }

    private fun setupRepositoryObservers() {
        val repo = getCategoriesUseCase.repository

        repo.onCategoriesListReceived = { lista ->
            Log.d(TAG, "Categorias recibidos: ${lista.size}")
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
                    loadCategories()
                } else {
                    if (requestIdRecibido == currentRequestId) {
                        exception.postValue("Error en operación Categoria: $accion")
                        currentRequestId = null
                    }
                }
            }

        repo.onOperationResult = resultHandler
        saveCategoryUseCase.repository.onOperationResult = resultHandler
        updateCategoryUseCase.repository.onOperationResult = resultHandler
        deleteCategoryUseCase.repository.onOperationResult = resultHandler
    }

    fun loadCategories() {
        _isLoading.postValue(true)
        if (socketManager.isConnected) {
            getCategoriesUseCase()
        } else {
            exception.postValue("Servidor desconectado")
            _isLoading.postValue(false)
        }
    }

    fun saveCategory(
        nombre: String,
        descripcion: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val category = Category(0, nombre, descripcion, estado)
        saveCategoryUseCase(category, currentRequestId!!)
    }

    fun updateCategory(
        id: Int,
        nombre: String,
        descripcion: String,
        estado: Boolean
    ) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        val category = Category(id, nombre, descripcion, estado)
        updateCategoryUseCase(category, currentRequestId!!)
    }

    fun deleteCategory(id: Int) {
        _isLoading.postValue(true)
        currentRequestId = UUID.randomUUID().toString()
        deleteCategoryUseCase(id, currentRequestId!!)
    }

    fun resetOperationStatus() {
        _operationSuccess.value = ""
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "CategoriesViewModel destruido")
    }
}
