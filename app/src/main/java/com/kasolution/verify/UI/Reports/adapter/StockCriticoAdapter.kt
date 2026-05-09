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

            // Personalización según el estado
            if (item.estado == "AGOTADO") {
                tvStockDetalle.text = "¡PRODUCTO AGOTADO!"
                tvStockDetalle.setTextColor(Color.parseColor("#D32F2F")) // Rojo fuerte

                // Si tienes un icono o el fondo del item, podrías cambiarlo aquí
                root.setBackgroundColor(Color.parseColor("#12FF0000")) // Un toque de rojo transparente de fondo
            } else {
                tvStockDetalle.text = "Stock crítico: ${item.stock_actual} unidades"
                tvStockDetalle.setTextColor(Color.parseColor("#F57C00")) // Naranja para advertencia

                root.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    override fun getItemCount() = productos.size
}