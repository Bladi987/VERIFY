package com.kasolution.verify.UI.Category.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import android.widget.Filter
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.UI.Category.adapter.utils.CategoriesDiffCallback
import com.kasolution.verify.domain.Inventory.model.Category
import com.kasolution.verify.databinding.CategoriesListItemBinding

class CategoriesAdapter(
    private val listaInicial: ArrayList<Category>,
    private val onClickListener: (Category) -> Unit,
    private val onLongClickListener: (Category, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>(), Filterable {

    private var selectedPosition: Int = -1
    private var categoriesFullList: List<Category> = listaInicial.toList()
    private var categoriesDisplayedList: MutableList<Category> = listaInicial.toMutableList()
    private val categoriesFilter: CategoryFilter = CategoryFilter()

    inner class ViewHolder(private val binding: CategoriesListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, position: Int) {
            // 1. Asignar Datos (Basado en tu tabla SQL)
            binding.tvCategoryName.text = category.nombre
            binding.tvCategoryDescription.text = category.descripcion ?: "No indica"
            if (category.estado) {
                // Estado ACTIVO: Fondo verde (rounded_tag_green)
                binding.tvCategoryStatus.apply {
                    text = itemView.context.getString(R.string.status_active) // Ej: "ACTIVO"
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_green)
                }
            } else {
                // Estado INACTIVO: Fondo gris o rojo (Asumiendo un color gris para inactivo)
                binding.tvCategoryStatus.apply {
                    text = itemView.context.getString(R.string.status_inactive) // Ej: "INACTIVO"
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_grey)
                }
            }
            // 2. Lógica de Selección (Visual)
            if (selectedPosition == position) {
                binding.root.foreground = ContextCompat.getColor(
                    itemView.context, R.color.selected_item_blue
                ).toDrawable()
            } else {
                binding.root.foreground = null
            }

            // 3. Manejar Eventos
            binding.root.setOnClickListener {
                onClickListener(category)
            }
            binding.root.setOnLongClickListener {view->
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                onLongClickListener(category, position)
                true
            }
        }
    }

    // --- Métodos de Gestión de Selección ---
    fun setSelectedItem(position: Int) {
        val previousSelection = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelection)
        notifyItemChanged(selectedPosition)
    }

    fun clearSelection() {
        val previousSelection = selectedPosition
        selectedPosition = -1
        notifyItemChanged(previousSelection)
    }

    // --- Métodos del Adapter ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CategoriesListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categoriesDisplayedList[position], position)
    }

    override fun getItemCount(): Int = categoriesDisplayedList.size

    // --- Lógica de Filtrado ---
    override fun getFilter(): Filter = categoriesFilter

    inner class CategoryFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().lowercase()

            val filteredList = if (charSearch.isEmpty()) {
                categoriesFullList
            } else {
                categoriesFullList.filter { category ->
                    // Filtrar por nombre o por DNI/RUC
                    category.nombre.lowercase().contains(charSearch)
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            categoriesDisplayedList.clear()
            categoriesDisplayedList.addAll(results?.values as List<Category>)
            notifyDataSetChanged()
            onDataChanged(categoriesDisplayedList.isEmpty())
        }
    }

    // --- Actualización de Datos ---
    fun updateList(newList: List<Category>) {
        val diffCallback = CategoriesDiffCallback(this.categoriesFullList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Actualizamos las referencias de datos
        this.categoriesFullList = newList.toList()
        this.categoriesDisplayedList.clear()
        this.categoriesDisplayedList.addAll(newList)

        // Aplicamos los cambios al Adapter de forma eficiente
        diffResult.dispatchUpdatesTo(this)

        // Notificamos si la lista está vacía
        onDataChanged(newList.isEmpty())
    }
}