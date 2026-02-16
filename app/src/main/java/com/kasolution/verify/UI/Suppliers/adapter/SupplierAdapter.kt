package com.kasolution.verify.UI.Suppliers.adapter

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
import com.kasolution.verify.UI.Suppliers.adapter.utils.SupplierDiffCallback
import com.kasolution.verify.UI.Suppliers.model.Supplier
import com.kasolution.verify.databinding.SupplierListItemBinding

class SupplierAdapter(
    private val listaInicial: ArrayList<Supplier>,
    private val onClickListener: (Supplier) -> Unit,
    private val onLongClickListener: (Supplier, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<SupplierAdapter.ViewHolder>(), Filterable {

    private var selectedPosition: Int = -1
    private var SuppliersFullList: List<Supplier> = listaInicial.toList()
    private var SuppliersDisplayedList: MutableList<Supplier> = listaInicial.toMutableList()
    private val SuppliersFilter: SupplierFilter = SupplierFilter()

    inner class ViewHolder(private val binding: SupplierListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(supplier: Supplier, position: Int) {
            // 1. Asignar Datos (Basado en tu tabla SQL)
            binding.tvNombre.text = supplier.nombre
            binding.tvTelefono.text = supplier.telefono ?: "Sin teléfono"
            binding.tvEmail.text = supplier.email ?: "Sin email"
            binding.tvDireccion.text = supplier.direccion ?: "Sin dirección"

            // Opcional: Generar iniciales para el círculo si lo usas

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
                onClickListener(supplier)
            }
            binding.root.setOnLongClickListener {view->
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                onLongClickListener(supplier, position)
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
        val binding = SupplierListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(SuppliersDisplayedList[position], position)
    }

    override fun getItemCount(): Int = SuppliersDisplayedList.size

    // --- Lógica de Filtrado ---
    override fun getFilter(): Filter = SuppliersFilter

    inner class SupplierFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().lowercase()

            val filteredList = if (charSearch.isEmpty()) {
                SuppliersFullList
            } else {
                SuppliersFullList.filter { supplier ->
                    // Filtrar por nombre o por DNI/RUC
                    supplier.nombre.lowercase().contains(charSearch) ||
                            supplier.email.lowercase().contains(charSearch)
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            SuppliersDisplayedList.clear()
            SuppliersDisplayedList.addAll(results?.values as List<Supplier>)
            notifyDataSetChanged()
            onDataChanged(SuppliersDisplayedList.isEmpty())
        }
    }

    // --- Actualización de Datos ---
    fun updateList(newList: List<Supplier>) {
        val diffCallback = SupplierDiffCallback(this.SuppliersFullList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Actualizamos las referencias de datos
        this.SuppliersFullList = newList.toList()
        this.SuppliersDisplayedList.clear()
        this.SuppliersDisplayedList.addAll(newList)

        // Aplicamos los cambios al Adapter de forma eficiente
        diffResult.dispatchUpdatesTo(this)

        // Notificamos si la lista está vacía
        onDataChanged(newList.isEmpty())
    }
}