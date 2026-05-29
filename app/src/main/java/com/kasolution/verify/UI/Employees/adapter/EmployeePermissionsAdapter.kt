package com.kasolution.verify.UI.Employees.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.databinding.ItemPermissionSwitchBinding
import com.kasolution.verify.domain.employees.model.EmployeePermission

class EmployeePermissionsAdapter(
    private var permisos: List<EmployeePermission> = emptyList(),
    private val onPermissionChanged: (Int, Boolean?) -> Unit
) : RecyclerView.Adapter<EmployeePermissionsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPermissionSwitchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(permiso: EmployeePermission) {
            // Formatear texto de forma segura
            binding.tvPermissionName.text = permiso.nombre.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            binding.tvPermissionDescription.text = "Módulo: ${permiso.modulo} [${permiso.slug}]"

            // 1. Apagar listener para evitar falsos disparos al reciclar o redibujar
            binding.switchPermission.setOnCheckedChangeListener(null)

            // 2. Calcular estado visual correcto
            val estadoFinal = permiso.excepcion ?: permiso.heredadoDeRol
            binding.switchPermission.isChecked = estadoFinal

            // 3. Pintar textos informativos y colores de manera estática
            if (permiso.excepcion != null) {
                binding.tvPermissionSource.text = "🔒 Ajuste manual aplicado"
                binding.tvPermissionSource.setTextColor(ContextCompat.getColor(itemView.context, R.color.selected_item_blue))
            } else {
                binding.tvPermissionSource.text = "👥 Heredado de su Rol (${if (permiso.heredadoDeRol) "Activo" else "Inactivo"})"
                binding.tvPermissionSource.setTextColor(ContextCompat.getColor(itemView.context, R.color.grey_text_secondary))
            }

            // 4. Capturar la interacción real del usuario
            binding.switchPermission.setOnCheckedChangeListener { _, isChecked ->
                permiso.excepcion = isChecked
                onPermissionChanged(permiso.idPermiso, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionSwitchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(permisos[position])
    }

    override fun getItemCount(): Int = permisos.size

    fun updateList(newList: List<EmployeePermission>) {
        this.permisos = newList
        notifyDataSetChanged()
    }

    // Función expuesta para que el Fragment pueda extraer el estado actual sin fallas
    fun getItems(): List<EmployeePermission> = permisos
}