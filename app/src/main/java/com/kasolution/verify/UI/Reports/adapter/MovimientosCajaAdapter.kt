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
            // 2. MOSTRAR CONCEPTOS: Ahora usamos el campo 'concepto' del log
            tvConcepto.text = item.concepto ?: "Venta General"

            // 3. MOSTRAR FECHA: Limpiamos las barras invertidas que envía el PHP
            tvFechaMovimiento.text = item.fecha?.replace("\\", "")

            // 4. LÓGICA DE COLORES Y MONTOS
            if (item.tipo == "INGRESO") {
                tvMonto.text = "+ S/ %.2f".format(item.monto)
                tvMonto.setTextColor(Color.parseColor("#2E7D32")) // Verde oscuro
            } else {
                tvMonto.text = "- S/ %.2f".format(item.monto)
                tvMonto.setTextColor(Color.RED)
            }
        }
    }

    override fun getItemCount() = movimientos.size
}