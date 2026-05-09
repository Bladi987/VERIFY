package com.kasolution.verify.UI.Reports.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.databinding.ItemMovimientoCajaBinding
import com.kasolution.verify.domain.reports.model.ReporteCajaMovimiento

class MovimientosCajaAdapter(private val movimientos: List<ReporteCajaMovimiento>) :
    RecyclerView.Adapter<MovimientosCajaAdapter.ViewHolder>() {

    // 1. ViewHolder: Usamos val para que sea accesible
    class ViewHolder(val binding: ItemMovimientoCajaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovimientoCajaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = movimientos[position]

        holder.binding.apply {
            tvConcepto.text = item.concepto ?: "Venta General"
            tvFechaMovimiento.text = item.fecha?.replace("\\", "")

            // Aseguramos que el monto sea positivo para el formateo,
            // nosotros controlamos el signo visualmente
            val montoAbsoluto = Math.abs(item.monto)

            // CORRECCIÓN: Si no es EGRESO, es un INGRESO (venga como Yape, Efectivo, etc.)
            val esEgreso = item.tipo.equals("EGRESO", ignoreCase = true)

            if (!esEgreso) {
                tvMonto.text = "+ S/ %.2f".format(montoAbsoluto)
                tvMonto.setTextColor(Color.parseColor("#2E7D32")) // Verde
            } else {
                tvMonto.text = "- S/ %.2f".format(montoAbsoluto)
                tvMonto.setTextColor(Color.RED) // Rojo
            }
        }
    }

    override fun getItemCount() = movimientos.size
}