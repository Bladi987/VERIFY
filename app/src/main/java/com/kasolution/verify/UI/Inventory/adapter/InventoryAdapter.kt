package com.kasolution.verify.UI.Inventory.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
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
import com.kasolution.verify.UI.Inventory.adapter.utils.InventoryDiffCallback
import com.kasolution.verify.UI.Inventory.model.Product
import com.kasolution.verify.databinding.ProductsListItemBinding


class InventoryAdapter(
    private val listaInicial: ArrayList<Product>,
    private val onClickListener: (Product) -> Unit,
    private val onLongClickListener: (Product, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>(), Filterable {

    private var selectedPosition: Int = -1
    private var inventoryFullList: List<Product> = listaInicial.toList()
    private var inventoryDisplayedList: MutableList<Product> = listaInicial.toMutableList()
    private val inventoryFilter: InventoryFilter = InventoryFilter()

    inner class ViewHolder(private val binding: ProductsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, position: Int) {
            // 1. Asignar Datos (Basado en tu tabla SQL)
            binding.tvNombreProducto.text = product.nombre
            binding.tvCodigo.text = "Código: ${product.codigo?:"No indica"}"
            binding.tvCategoria.text = "${product.nombreCategoria ?: "---"}"
            binding.tvStock.text = "Stock: ${product.stock} ${product.unidadMedida}"
            binding.tvPrecio.text = "S/ ${String.format("%.2f", product.precioVenta)}"

            binding.root.isEnabled = true
            binding.root.alpha = 1.0f
            binding.tvStock.setTypeface(null, android.graphics.Typeface.NORMAL)
            // 2. LÓGICA DE ESTADO (Disponible vs No Disponible)
            if (!product.estado ) {
                // --- EFECTO PRODUCTO DESACTIVADO ---
                binding.root.alpha = 0.5f // Lo hace ver "borroso" o semi-transparente
                binding.indicatorStock.setBackgroundColor(Color.GRAY)
                binding.tvStock.text = "NO DISPONIBLE"
                binding.priceContainer.setBackgroundResource(R.drawable.bg_price_disabled)
                binding.tvPrecio.setTextColor(Color.GRAY)
                binding.tvStock.setTextColor(Color.GRAY)
                binding.tvStock.setTypeface(null, android.graphics.Typeface.ITALIC)

                // Deshabilitar clics si es necesario (Opcional)
                binding.root.isEnabled = false
            } else {
                // --- PRODUCTO ACTIVO (Lógica de Stock que ya teníamos) ---
                binding.tvStock.text = "Stock: ${product.stock} ${product.unidadMedida}"
                binding.priceContainer.setBackgroundResource(R.drawable.bg_price_light)
                binding.tvPrecio.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue_corporative_primary))
                val stockMinimo = 5
                if (product.stock <= stockMinimo) {
                    // Color Rojo para alerta
                    val colorAlert = ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)

                    binding.indicatorStock.setBackgroundColor(colorAlert)
                    binding.tvStock.setTextColor(colorAlert)
                    binding.tvStock.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    // Color Corporativo (Azul) para stock normal
                    val colorNormal = ContextCompat.getColor(itemView.context, R.color.blue_corporative_primary)
                    val colorTextNormal = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)

                    binding.indicatorStock.setBackgroundColor(colorNormal)
                    binding.tvStock.setTextColor(colorTextNormal)

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
                onClickListener(product)
            }
            binding.root.setOnLongClickListener {view->
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                onLongClickListener(product, position)
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
        val binding = ProductsListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(inventoryDisplayedList[position], position)
    }

    override fun getItemCount(): Int = inventoryDisplayedList.size

    // --- Lógica de Filtrado ---
    override fun getFilter(): Filter = inventoryFilter

    inner class InventoryFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().lowercase().trim()

            val filteredList = if (charSearch.isEmpty() || charSearch == "todos") {
                inventoryFullList
            } else {
                inventoryFullList.filter { product ->
                    // 1. Buscamos en el Nombre
                    val matchesName = product.nombre.lowercase().contains(charSearch)

                    // 2. Buscamos en el Código
                    val matchesCode = product.codigo?.lowercase()?.contains(charSearch) ?: false

                    // 3. Buscamos en la Categoría (Esto conecta con tus Chips)
                    val matchesCategory = product.nombreCategoria?.lowercase() == charSearch

                    // Si coincide con cualquiera de los tres, se queda en la lista
                    matchesName || matchesCode || matchesCategory
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // Usamos DiffUtil para que la transición sea suave en lugar de notifyDataSetChanged()
            val newList = results?.values as List<Product>

            // Creamos un callback para comparar la lista actual con la nueva filtrada
            val diffCallback = InventoryDiffCallback(inventoryDisplayedList, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            inventoryDisplayedList.clear()
            inventoryDisplayedList.addAll(newList)

            diffResult.dispatchUpdatesTo(this@InventoryAdapter)

            onDataChanged(inventoryDisplayedList.isEmpty())
        }
    }

    // --- Actualización de Datos ---
    fun updateList(newList: List<Product>) {
        val diffCallback = InventoryDiffCallback(this.inventoryFullList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Actualizamos las referencias de datos
        this.inventoryFullList = newList.toList()
        this.inventoryDisplayedList.clear()
        this.inventoryDisplayedList.addAll(newList)

        // Aplicamos los cambios al Adapter de forma eficiente
        diffResult.dispatchUpdatesTo(this)

        // Notificamos si la lista está vacía
        onDataChanged(newList.isEmpty())
    }

}