package com.kasolution.verify.UI.Sales.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.databinding.ItemSalesHistoryBinding

class SalesHistoryAdapter(
    private var sales: List<Map<String, Any>>,
    private val onItemClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<SalesHistoryAdapter.HistoryViewHolder>() {

    // ViewHolder ahora recibe el Binding directamente
    inner class HistoryViewHolder(val binding: ItemSalesHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemSalesHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val sale = sales[position]

        with(holder.binding) {
            // Extracción de datos con seguridad
            val idVenta = sale["id_venta"].toString()
            val total = sale["total"]?.toString()?.toDoubleOrNull() ?: 0.0
            val cliente = sale["cliente_nombre"]?.toString() ?: "Público General"
            val fecha = sale["fecha"]?.toString() ?: "--/--/--"
            val estado = sale["estado"]?.toString() ?: "PAGADO"
            val metodoPago = sale["metodo_pago"]?.toString() ?: "EFECTIVO"

            // Asignación a las vistas mediante el binding
            tvIdVenta.text = "#$idVenta"
            tvFechaVenta.text = fecha
            tvNombreCliente.text = cliente
            tvTotalVenta.text = "S/ ${String.format("%.2f", total)}"
            tvEstado.text = estado

            tvMetodoPago.text = metodoPago.uppercase()
            // Lógica de colores según el estado
            if (estado == "ANULADO") {
                tvEstado.setBackgroundResource(R.drawable.bg_status_annulled)
            } else {
                tvEstado.setBackgroundResource(R.drawable.bg_status_paid)
            }

            // Evento de click largo para anulación
//            root.setOnLongClickListener {
//                if (estado != "ANULADO") {
//                    onLongClick(idVenta.toInt())
//                }
//                true
//            }
            root.setOnClickListener {
                onItemClick(sale)
            }
        }
    }

    override fun getItemCount() = sales.size

    fun updateList(newList: List<Map<String, Any>>) {
        sales = newList
        notifyDataSetChanged()
    }
}