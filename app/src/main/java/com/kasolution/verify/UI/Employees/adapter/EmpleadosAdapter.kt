package com.kasolution.verify.UI.Employees.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.domain.employees.model.Employee
import com.kasolution.verify.databinding.EmpleadosListItemBinding
import android.widget.Filter
import androidx.core.graphics.drawable.toDrawable

class EmpleadosAdapter(
    private val listaInicial: ArrayList<Employee>,
    private val onClickListener: (Employee) -> Unit,
    private val onLongClickListener: (Employee, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<EmpleadosAdapter.ViewHolder>(), Filterable {
    private var selectedPosition: Int = -1
    private var empleadosFullList: List<Employee> = listaInicial.toList()
    private var empleadosDisplayedList: MutableList<Employee> = listaInicial.toMutableList()
    private val empleadosFilter: EmpleadoFilter = EmpleadoFilter()

    //    private val onDataChanged: (isEmpty: Boolean) -> Unit
    inner class ViewHolder(private val binding: EmpleadosListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(empleado: Employee, position: Int) {
            // 1. Asignar Texto
            binding.tvInitials.text = empleado.initials
            binding.tvEmployeeName.text = empleado.nombre
            binding.tvEmployeeBranch.text = empleado.sucursalNombre
            binding.tvEmployeeRole.text = empleado.nombreRol.ifEmpty { "Sin rol asignado" }


            if (!empleado.telefono.isNullOrEmpty()) {
                binding.tvEmployeePhone.visibility = View.VISIBLE
                binding.tvEmployeePhone.text = "📞 ${empleado.telefono}"
            } else {
                binding.tvEmployeePhone.visibility = View.GONE
            }

            // 4. NUEVO: Gestión defensiva de Correo (Se oculta si es nulo o vacío)
            if (!empleado.correo.isNullOrEmpty()) {
                binding.tvEmployeeEmail.visibility = View.VISIBLE
                binding.tvEmployeeEmail.text = "✉️ ${empleado.correo}"
            } else {
                binding.tvEmployeeEmail.visibility = View.GONE
            }

            // 5. Estado Operacional (Tag Activo / Inactivo)
            if (empleado.estado) {
                binding.tvEmployeeStatus.apply {
                    text = itemView.context.getString(R.string.status_active)
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_green)
                }
            } else {
                binding.tvEmployeeStatus.apply {
                    text = itemView.context.getString(R.string.status_inactive)
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_grey)
                }
            }

            // Manejo de la selección visual por Foreground
            if (selectedPosition == position) {
                binding.root.foreground =
                    ContextCompat.getColor(itemView.context, R.color.selected_item_blue)
                        .toDrawable()
            } else {
                binding.root.foreground = null
            }

            // Gestores de Clics nativos
            binding.root.setOnClickListener {
                onClickListener(empleado)
            }
            binding.root.setOnLongClickListener { view ->
                val imm =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                onLongClickListener(empleado, position)
                true
            }
        }
    }

    // Método para marcar el ítem como seleccionado
    fun setSelectedItem(position: Int) {
        val previousSelection = selectedPosition
        selectedPosition = position
        // Refrescamos el ítem anterior y el nuevo para cambiar el color
        notifyItemChanged(previousSelection)
        notifyItemChanged(selectedPosition)
    }

    // Método para limpiar la selección
    fun clearSelection() {
        val previousSelection = selectedPosition
        selectedPosition = -1
        notifyItemChanged(previousSelection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Infla el layout del ítem usando View Binding para un acceso limpio a las vistas
        val binding = EmpleadosListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(empleadosDisplayedList[position], position)
    }

    override fun getItemCount(): Int {
        return empleadosDisplayedList.size
    }

    override fun getFilter(): Filter = empleadosFilter

    inner class EmpleadoFilter : Filter() {

        // 1. Ejecuta el filtrado en un hilo secundario
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().toLowerCase().trim()

            val filteredList = if (charSearch.isEmpty()) {
                // Si el término de búsqueda está vacío, mostrar la lista completa
                empleadosFullList
            } else {
                // Si hay un término de búsqueda, filtrar la lista completa
                empleadosFullList.filter { empleado ->
                    // Búsqueda por Nombre Completo o Nombre de Usuario
                    empleado.nombre.toLowerCase().contains(charSearch) ||
                            empleado.usuario.toLowerCase().contains(charSearch) ||
                            empleado.nombreRol.toLowerCase().contains(charSearch)
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        // 2. Publica los resultados en el hilo principal (UI Thread)
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // Actualizar la lista mostrada y notificar al RecyclerView
            empleadosDisplayedList.clear()
            empleadosDisplayedList.addAll(results?.values as List<Employee>)
            notifyDataSetChanged()
            onDataChanged(empleadosDisplayedList.isEmpty())
        }
    }

    fun updateList(newList: List<Employee>) {
        // Actualizamos ambas listas para que el filtrado siga funcionando
        this.empleadosFullList = newList.toList()
        this.empleadosDisplayedList.clear()
        this.empleadosDisplayedList.addAll(newList)
        notifyDataSetChanged()
        //Notificamos a la Activity si los nuevos datos están vacíos
        onDataChanged(newList.isEmpty())
    }
}