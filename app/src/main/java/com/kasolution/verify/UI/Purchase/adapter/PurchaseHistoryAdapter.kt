package com.kasolution.verify.UI.Purchase.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.databinding.ItemPurchaseHistoryBinding

class PurchaseHistoryAdapter(
    private var purchases: List<Map<String, Any>>,
    private val onItemClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseViewHolder>() {

    inner class PurchaseViewHolder(val binding: ItemPurchaseHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val binding = ItemPurchaseHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PurchaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]

        with(holder.binding) {
            // 1. Extracción de datos del Map (PHP names)
            val idCompra = purchase["id_compra"].toString()
            val proveedor = purchase["proveedor_nombre"]?.toString() ?: "Proveedor Desconocido"
            val fecha = purchase["fecha"]?.toString() ?: "--/--/--"
            val total = purchase["total"]?.toString()?.toDoubleOrNull() ?: 0.0
            val estado = purchase["estado"]?.toString() ?: "COMPLETADO"
            val empleado = purchase["empleado_nombre"]?.toString() ?: "Usuario"

            // 2. Asignación básica
            tvFecha.text = fecha
            tvProveedorNombre.text = proveedor
            tvEmpleadoNombre.text = "Por: $empleado"
            tvTotalCompra.text = "S/ ${String.format("%.2f", total)}"
            tvEstadoBadge.text = estado

            // 3. Lógica visual según el estado (ESTILO COMPRAS)
            if (estado == "ANULADO") {
                // Estilo Rojo / Tachado
                tvEstadoBadge.setBackgroundResource(R.drawable.bg_status_annulled)
                tvTotalCompra.setTextColor(Color.RED)
                // Aplicar tachado al precio
                tvTotalCompra.paintFlags = tvTotalCompra.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                // Opacidad reducida para indicar registro inactivo
                container.alpha = 0.6f
            } else {
                // Estilo Verde / Normal
                tvEstadoBadge.setBackgroundResource(R.drawable.bg_button_green)
                tvTotalCompra.setTextColor(Color.parseColor("#2E7D32")) // Verde compras
                // Quitar tachado si existía por reciclaje de vista
                tvTotalCompra.paintFlags = tvTotalCompra.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                container.alpha = 1.0f
            }

            // 4. Navegación al detalle
            root.setOnClickListener {
                onItemClick(purchase)
            }
        }
    }

    override fun getItemCount() = purchases.size

    fun updateList(newList: List<Map<String, Any>>) {
        purchases = newList
        notifyDataSetChanged()
    }
}