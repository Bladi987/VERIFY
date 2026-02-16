package com.kasolution.verify.UI.Category

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kasolution.verify.R
import com.kasolution.verify.UI.Category.adapter.CategoriesAdapter
import com.kasolution.verify.UI.Category.fragment.CategoryFormDialogFragment
import com.kasolution.verify.UI.Category.model.Category
import com.kasolution.verify.UI.Category.viewModel.CategoriesViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityCategoriesBinding
import kotlin.getValue

class CategoriesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var adapter: CategoriesAdapter
    private lateinit var lista: ArrayList<Category>
    private var selectedCategory: Category? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val viewModel: CategoriesViewModel by viewModels {
        AppProvider.provideCategoriesViewModelFactory()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutOptions)
        bottomSheetBehavior.apply {
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN // Inicia oculto
        }
        setSupportActionBar(binding.actionBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        lista = ArrayList()
        initRecycler()
        initBottonSheet()
        setupObservers()
        viewModel.loadCategories()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ejecutar el filtro cada vez que el texto cambie
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnAddCategory.setOnClickListener {
            // 1. Instanciamos el fragmento
            val dialogFragment = CategoryFormDialogFragment()
            // El "CategoryTag" es solo una etiqueta para identificar al fragmento en memoria
            dialogFragment.show(supportFragmentManager, "CategoryTag")
        }

        binding.btnEditOption.setOnClickListener {
            selectedCategory?.let { category ->
                // Abrimos el diálogo enviando el objeto categoria
                val dialog = CategoryFormDialogFragment.newInstance(category)
                dialog.show(supportFragmentManager, "EditCategory")
                hideOptions()
            }
        }

        binding.btnDeleteOption.setOnClickListener {
            selectedCategory?.let { cli ->
                DialogHelper.showConfirmation(
                    this,
                    "Eliminar Categoria",
                    "¿Estás seguro de que deseas eliminar ${cli.nombre}?",
                    onConfirm = {
                        viewModel.deleteCategory(cli.id)
                        hideOptions()
                    })
            }
        }
        binding.btnRetry.setOnClickListener {
            viewModel.loadCategories()
        }

        binding.btnSearch.setOnClickListener {
            alternarTitulo()
        }
    }
    private fun initRecycler() {
        lmanager = LinearLayoutManager(this)
        adapter = CategoriesAdapter(
            listaInicial = lista,
            onClickListener = { category -> onItemClicListener(category) },
            onLongClickListener = { category, position -> showOptionsFor(category, position) },
            onDataChanged = { isEmpty -> toggleEmptyState(isEmpty) }
        )
        binding.rvCategories.layoutManager = lmanager
        binding.rvCategories.adapter = adapter

        adapter.onDataChanged(lista.isEmpty())
    }
    private fun initBottonSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Aquí puedes realizar acciones cuando el usuario termine de deslizarlo hacia abajo
                    adapter.clearSelection()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Opcional: puedes cambiar la opacidad de un fondo oscuro aquí
            }
        })
    }
    private fun onItemClicListener(category: Category) {
        //Toast.makeText(this, "${category.nombre} - ${category.usuario}", Toast.LENGTH_SHORT).show()
        hideOptions()
    }
    private fun showOptionsFor(category: Category, position: Int) {
        binding.etSearch.clearFocus()
        selectedCategory = category
        binding.tvSelectedName.text = category.nombre
        adapter.setSelectedItem(position)

        if (binding.layoutOptions.visibility != View.VISIBLE) {
            binding.layoutOptions.visibility = View.VISIBLE
        }

        // En lugar de usar .animate(), usamos los estados del Behavior
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            // Animación sutil de feedback si ya estaba abierto (cambio de empleado)
            binding.tvSelectedName.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100)
                .withEndAction {
                    binding.tvSelectedName.animate().scaleX(1f).scaleY(1f).start()
                }.start()
        }
    }

    private fun hideOptions() {
        adapter.clearSelection()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
    private fun setupObservers() {
        viewModel.categoriesList.observe(this) { lista ->
            val emptyList = lista.isNullOrEmpty()
            mostrarMensajeOrLista(true, emptyList)
            adapter.updateList(lista ?: emptyList())
        }
        viewModel.exception.observe(this) { error ->
            mostrarMensajeOrLista(exito = false, mensaje = error)
            ToastHelper.showCustomToast(binding.root, error, false)
        }
        viewModel.operationSuccess.observe(this) { action ->
            if (action.isNullOrEmpty()) return@observe
            val mensaje = when (action) {
                "CATEGORY_SAVE" -> "¡Categoria registrado con éxito!"
                "CATEGORY_UPDATE" -> "Datos actualizados correctamente"
                "CATEGORY_DELETE" -> "Categoria eliminado"
                else -> ""
            }
            ToastHelper.showCustomToast(binding.root, mensaje, true)
            binding.root.postDelayed({
                viewModel.resetOperationStatus()
            }, 100)
        }

        viewModel.isLoading.observe(this) { loading ->
            ProgressHelper.showProgress(this, "Cargando...")
            if (!loading) ProgressHelper.hideProgress()
        }
    }
    fun mostrarMensajeOrLista(
        exito: Boolean,
        lista: Boolean = false,
        mensaje: String = "Sin datos que mostrar"
    ) {
        if (exito) {
            //la peticion fue exitosa, ahora validamos si la lista esta vacio
            if (lista) {
                //si la lista esta vacio mostramos el mensaje
                binding.rvCategories.isVisible = false
                binding.layoutMessage.isVisible = true
                binding.tvTitleMeassage.text = mensaje
                binding.btnRetry.isVisible = false
                binding.btnAddCategory.isEnabled = true
                binding.btnSearch.isEnabled = true
            } else {
                //mostramos el recyclerView
                binding.rvCategories.isVisible = true
                binding.layoutMessage.isVisible = false
                binding.btnAddCategory.isEnabled = true
                binding.btnSearch.isEnabled = true
            }
        } else {
            //mostramos mensaje ya sea sin datos o error
            binding.rvCategories.isVisible = false
            binding.layoutMessage.isVisible = true
            binding.tvTitleMeassage.text = mensaje
            binding.btnRetry.isVisible = true
            binding.btnAddCategory.isEnabled = false
            binding.btnSearch.isEnabled = false
        }
    }
    fun alternarTitulo() {
        val animInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        val animOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_top)
        val animInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        val animOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)
        if (binding.etSearch.visibility == View.VISIBLE) {
            binding.etSearch.startAnimation(animOutRight)
            binding.ivSearch.setImageResource(R.drawable.ic_search)
            binding.etSearch.text?.clear()
            binding.etSearch.visibility = View.GONE
            binding.tvTitle.visibility = View.VISIBLE
            binding.tvTitle.startAnimation(animInLeft)
        } else {
            binding.tvTitle.startAnimation(animOutLeft)
            binding.tvTitle.visibility = View.GONE
            binding.etSearch.startAnimation(animInRight)
            binding.etSearch.visibility = View.VISIBLE
            binding.ivSearch.setImageResource(R.drawable.ic_close)
            binding.etSearch.requestFocus()
            val inputMethodManager =
                this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun toggleEmptyState(isEmpty: Boolean) {
        binding.rvCategories.isVisible = !isEmpty
//        binding.tvEmptyState.isVisible = isEmpty
    }
    override fun onSupportNavigateUp(): Boolean {
        // Esto simula presionar el botón físico/virtual de "atrás" del teléfono
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}