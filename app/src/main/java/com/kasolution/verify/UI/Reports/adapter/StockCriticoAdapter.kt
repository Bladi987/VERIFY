package com.kasolution.verify.UI.Reports.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.UI.Reports.model.ProductoCritico
import com.kasolution.verify.databinding.ItemProductoCriticoBinding

class StockCriticoAdapter(private val productos: List<ProductoCritico>) :
    RecyclerView.Adapter<StockCriticoAdapter.ViewHolder>() {

    // Cambiamos 'private val' a 'val' para que sea accesible desde el adapter
    class ViewHolder(val binding: ItemProductoCriticoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoCriticoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productos[position]

        holder.binding.apply {
            tvNombreProducto.text = item.nombre
            tvStockDetalle.text = "Stock actual: ${item.stock_actual}"

            // Opcional: Resaltar en rojo si el stock es crítico (ej. menor a 5)
            if (item.stock_actual <= 5) {
                tvStockDetalle.setTextColor(Color.RED)
            } else {
                tvStockDetalle.setTextColor(Color.GRAY)
            }
        }
    }

    override fun getItemCount() = productos.size
}