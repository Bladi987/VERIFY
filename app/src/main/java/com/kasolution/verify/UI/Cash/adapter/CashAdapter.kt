package com.kasolution.verify.UI.Cash.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.databinding.ItemCashMovementBinding
import java.util.Locale

class CashAdapter(
    private var movementsList: MutableList<Map<String, Any>>,
    private val onClickListener: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<CashAdapter.ViewHolder>(), Filterable {

    private var fullList: List<Map<String, Any>> = movementsList.toList()
    private var displayedList: MutableList<Map<String, Any>> = movementsList.toMutableList()

    inner class ViewHolder(private val binding: ItemCashMovementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movement: Map<String, Any>) {
            // 1. Extraer datos con seguridad total
            val tipo = movement["tipo"]?.toString() ?: ""
            val motivo = movement["motivo"]?.toString() ?: "Sin motivo"
            val hora = movement["fecha"]?.toString() ?: "--:--"
            val monto = movement["monto"]?.toString()?.toDoubleOrNull() ?: 0.0

            binding.tvMovementReason.text =
                if (motivo.contains("Venta #", true)) motivo else motivo.capitalize()
            // Si la fecha viene como "2026-04-03 10:58:20", podrías tomar solo la hora si prefieres
            binding.tvMovementTime.text = hora.split(" ").lastOrNull() ?: hora

            val context = itemView.context

            binding.ivMovementIcon.clearColorFilter()

            // 2. Lógica de Colores e Iconos según el Tipo
            when (tipo.uppercase()) {
                "YAPE" -> {
                    setupItem(
                        icon = R.drawable.ic_yape, // Asegúrate de tener este drawable
                        colorStr = "#8E44AD", // Morado Yape
                        prefix = "+ S/ ",
                        amount = monto,
                        keepOriginalColors = true
                    )
                    binding.tvMovementReason.text = "Venta Yape"
                }

                "PLIN" -> {
                    setupItem(
                        icon = R.drawable.ic_plin, // Asegúrate de tener este drawable
                        colorStr = "#00B4FF", // Celeste Plin
                        prefix = "+ S/ ",
                        amount = monto,
                        keepOriginalColors = true
                    )
                    binding.tvMovementReason.text = "Venta Plin"
                }

                "TARJETA" -> {
                    setupItem(
                        icon = R.drawable.ic_tarjeta,
                        colorStr = "#34495E", // Gris oscuro/Azul profesional
                        prefix = "+ S/ ",
                        amount = monto,
                        keepOriginalColors = true
                    )
                    binding.tvMovementReason.text = "Venta Tarjeta"
                }

                "EFECTIVO", "VENTA" -> {
                    setupItem(
                        icon = R.drawable.ic_cash, // Icono de billetes
                        colorInt = ContextCompat.getColor(context, R.color.semantic_success_green),
                        prefix = "+ S/ ",
                        amount = monto,
                        keepOriginalColors = true
                    )
                }

                "TRANSFERENCIA" -> {
                    setupItem(
                        icon = R.drawable.ic_account_balance,
                        colorStr = "#F39C12", // Naranja banco
                        prefix = "+ S/ ",
                        amount = monto
                    )
                }

                "EGRESO", "COMPRA" -> {
                    setupItem(
                        icon = R.drawable.ic_arrow_downward,
                        colorInt = ContextCompat.getColor(context, R.color.semantic_error_red),
                        prefix = "- S/ ",
                        amount = monto
                    )
                }

                "APERTURA" -> {
                    setupItem(
                        icon = R.drawable.ic_cash_register,
                        colorInt = ContextCompat.getColor(
                            context,
                            R.color.blue_corporative_primary
                        ),
                        prefix = "S/ ",
                        amount = monto
                    )
                }

                else -> {
                    setupItem(
                        icon = R.drawable.ic_info,
                        colorInt = Color.GRAY,
                        prefix = "S/ ",
                        amount = monto
                    )
                }
            }

            binding.root.setOnClickListener { onClickListener(movement) }
        }

        private fun setupItem(
            icon: Int,
            colorInt: Int? = null,
            colorStr: String? = null,
            prefix: String,
            amount: Double,
            keepOriginalColors: Boolean = false
        ) {
            val finalColor = colorStr?.let { Color.parseColor(it) } ?: colorInt ?: Color.BLACK

            binding.ivMovementIcon.setImageResource(icon)

            if (keepOriginalColors) {
                // Quitamos cualquier filtro previo para ver los colores reales del logo
                binding.ivMovementIcon.clearColorFilter()
            } else {
                // Aplicamos el color solo a los iconos genéricos
                binding.ivMovementIcon.setColorFilter(finalColor)
            }

            binding.tvMovementAmount.text = "$prefix${String.format(Locale.US, "%.2f", amount)}"
            binding.tvMovementAmount.setTextColor(finalColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCashMovementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayedList[position])
    }

    override fun getItemCount(): Int = displayedList.size

    // --- Actualización de Datos ---
    fun updateList(newList: List<Map<String, Any>>) {
        this.fullList = newList
        this.displayedList = newList.toMutableList()
        notifyDataSetChanged() // Al ser una lista de historial, un refresh simple suele bastar
    }

    // --- Lógica de Filtrado (Opcional por si buscas movimientos) ---
    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint.toString().lowercase()
            val filtered = if (query.isEmpty()) fullList else {
                fullList.filter {
                    (it["motivo"] as? String)?.lowercase()?.contains(query) == true ||
                            (it["tipo"] as? String)?.lowercase()?.contains(query) == true
                }
            }
            return FilterResults().apply { values = filtered }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            displayedList =
                (results?.values as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
            notifyDataSetChanged()
        }
    }
}