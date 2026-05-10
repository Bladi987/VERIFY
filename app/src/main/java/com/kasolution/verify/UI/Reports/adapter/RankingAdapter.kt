package com.kasolution.verify.UI.Reports.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.UI.Reports.model.ProductoRanking
import com.kasolution.verify.databinding.ItemProductoRankingBinding

class RankingAdapter(private val lista: List<ProductoRanking>) :
    RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProductoRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.binding.apply {
            tvPosicion.text = "${position + 1}°"
            tvNombre.text = item.nombre
            tvCantidad.text = "${item.cantidad_vendida} vendidos"
            tvMonto.text = "S/ ${"%.2f".format(item.total_recaudado)}"

            // Resaltar el Top 3
            val color = if (position < 3) "#FFD700" else "#808080" // Dorado para el top 3
            tvPosicion.setTextColor(Color.parseColor(color))
        }
    }

    override fun getItemCount() = lista.size
}