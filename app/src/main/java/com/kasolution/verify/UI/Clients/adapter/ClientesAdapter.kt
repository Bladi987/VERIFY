package com.kasolution.verify.UI.Clients.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import android.widget.Filter
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.UI.Clients.adapter.utils.ClienteDiffCallback
import com.kasolution.verify.UI.Clients.model.Cliente
import com.kasolution.verify.databinding.ClientesListItemBinding

class ClientesAdapter(
    private val listaInicial: ArrayList<Cliente>,
    private val onClickListener: (Cliente) -> Unit,
    private val onLongClickListener: (Cliente, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<ClientesAdapter.ViewHolder>(), Filterable {

    private var selectedPosition: Int = -1
    private var clientesFullList: List<Cliente> = listaInicial.toList()
    private var clientesDisplayedList: MutableList<Cliente> = listaInicial.toMutableList()
    private val clientesFilter: ClienteFilter = ClienteFilter()

    inner class ViewHolder(private val binding: ClientesListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cliente: Cliente, position: Int) {
            // 1. Asignar Datos (Basado en tu tabla SQL)
            binding.tvNombre.text = cliente.nombre
            binding.tvDniRuc.text = "DNI/RUC: ${cliente.dniRuc ?: "---"}"
            binding.tvTelefono.text = cliente.telefono ?: "Sin teléfono"
            binding.tvDireccion.text = cliente.direccion ?: "Sin dirección"

            // Opcional: Generar iniciales para el círculo si lo usas
            // binding.tvInitials.text = cliente.nombre.take(1).uppercase()

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
                onClickListener(cliente)
            }
            binding.root.setOnLongClickListener {
                onLongClickListener(cliente, position)
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
        val binding = ClientesListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(clientesDisplayedList[position], position)
    }

    override fun getItemCount(): Int = clientesDisplayedList.size

    // --- Lógica de Filtrado ---
    override fun getFilter(): Filter = clientesFilter

    inner class ClienteFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().lowercase()

            val filteredList = if (charSearch.isEmpty()) {
                clientesFullList
            } else {
                clientesFullList.filter { cliente ->
                    // Filtrar por nombre o por DNI/RUC
                    cliente.nombre.lowercase().contains(charSearch) ||
                            (cliente.dniRuc?.lowercase()?.contains(charSearch) ?: false)
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clientesDisplayedList.clear()
            clientesDisplayedList.addAll(results?.values as List<Cliente>)
            notifyDataSetChanged()
            onDataChanged(clientesDisplayedList.isEmpty())
        }
    }

    // --- Actualización de Datos ---
    fun updateList(newList: List<Cliente>) {
        val diffCallback = ClienteDiffCallback(this.clientesFullList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Actualizamos las referencias de datos
        this.clientesFullList = newList.toList()
        this.clientesDisplayedList.clear()
        this.clientesDisplayedList.addAll(newList)

        // Aplicamos los cambios al Adapter de forma eficiente
        diffResult.dispatchUpdatesTo(this)

        // Notificamos si la lista está vacía
        onDataChanged(newList.isEmpty())
    }
}