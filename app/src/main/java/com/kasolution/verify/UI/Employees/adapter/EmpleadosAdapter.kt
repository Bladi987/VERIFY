package com.kasolution.verify.UI.Employees.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.UI.Employees.model.Empleado
import com.kasolution.verify.databinding.EmpleadosListItemBinding
import android.widget.Filter
import androidx.core.graphics.drawable.toDrawable

class EmpleadosAdapter(
    private val listaInicial: ArrayList<Empleado>,
    private val onClickListener: (Empleado) -> Unit,
    private val onLongClickListener: (Empleado, Int) -> Unit,
    val onDataChanged: (isEmpty: Boolean) -> Unit
) : RecyclerView.Adapter<EmpleadosAdapter.ViewHolder>(), Filterable {
    private var selectedPosition: Int = -1
    private var empleadosFullList: List<Empleado> = listaInicial.toList()
    private var empleadosDisplayedList: MutableList<Empleado> = listaInicial.toMutableList()
    private val empleadosFilter: EmpleadoFilter = EmpleadoFilter()

    //    private val onDataChanged: (isEmpty: Boolean) -> Unit
    inner class ViewHolder(private val binding: EmpleadosListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(empleado: Empleado,position: Int) {
            // 1. Asignar Texto
            binding.tvInitials.text = empleado.initials
            binding.tvEmployeeName.text = empleado.nombre
            binding.tvEmployeeRole.text = empleado.rol.capitalize()


            // 2. Lógica del Tag de Estado (Usa los Drawables y colores definidos)
            if (empleado.estado) {
                // Estado ACTIVO: Fondo verde (rounded_tag_green)
                binding.tvEmployeeStatus.apply {
                    text = itemView.context.getString(R.string.status_active) // Ej: "ACTIVO"
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_green)
                    // Podrías necesitar configurar el color de texto si no está en el XML
                    // textColor = ContextCompat.getColor(context, R.color.white_pure)
                }
            } else {
                // Estado INACTIVO: Fondo gris o rojo (Asumiendo un color gris para inactivo)
                binding.tvEmployeeStatus.apply {
                    text = itemView.context.getString(R.string.status_inactive) // Ej: "INACTIVO"
                    // Necesitas definir un drawable/color para inactivo, por ejemplo rounded_tag_grey
                    background = ContextCompat.getDrawable(context, R.drawable.rounded_tag_grey)
                }
            }
            if (selectedPosition == position) {
                // Color cuando está seleccionado (puedes usar un azul muy claro o el color primario con alpha)
                binding.root.foreground =
                    ContextCompat.getColor(itemView.context, R.color.selected_item_blue)
                        .toDrawable()
            } else {
                // Color normal (transparente o blanco)
                binding.root.foreground = null
            }

            // 3. Manejar el Clic en el Ítem
            binding.root.setOnClickListener {
                onClickListener(empleado)
            }
            binding.root.setOnLongClickListener {
                onLongClickListener(empleado,position)
                true // Retornamos true para indicar que consumimos el evento
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
        holder.bind(empleadosDisplayedList[position],position)
    }

    override fun getItemCount(): Int {
        return empleadosDisplayedList.size
    }


    override fun getFilter(): Filter = empleadosFilter

    inner class EmpleadoFilter : Filter() {

        // 1. Ejecuta el filtrado en un hilo secundario
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString().toLowerCase()

            val filteredList = if (charSearch.isEmpty()) {
                // Si el término de búsqueda está vacío, mostrar la lista completa
                empleadosFullList
            } else {
                // Si hay un término de búsqueda, filtrar la lista completa
                empleadosFullList.filter { empleado ->
                    // Búsqueda por Nombre Completo o Nombre de Usuario
                    empleado.nombre.toLowerCase().contains(charSearch) ||
                            empleado.usuario.toLowerCase().contains(charSearch)
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
            empleadosDisplayedList.addAll(results?.values as List<Empleado>)
            notifyDataSetChanged()
            onDataChanged(empleadosDisplayedList.isEmpty())
        }
    }

    fun updateList(newList: List<Empleado>) {
        // Actualizamos ambas listas para que el filtrado siga funcionando
        this.empleadosFullList = newList.toList()
        this.empleadosDisplayedList.clear()
        this.empleadosDisplayedList.addAll(newList)
        notifyDataSetChanged()
        //Notificamos a la Activity si los nuevos datos están vacíos
        onDataChanged(newList.isEmpty())
    }
}