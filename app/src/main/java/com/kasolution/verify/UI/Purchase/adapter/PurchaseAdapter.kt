package com.kasolution.verify.UI.Purchase.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.databinding.ItemPurchaseBinding
import com.kasolution.verify.UI.Purchase.model.PurchaseItem
import java.util.Locale

class PurchaseAdapter(
    private var purchaseList: MutableList<PurchaseItem>,
    private val onIncrementClick: (PurchaseItem, Int) -> Unit,
    private val onDecrementClick: (PurchaseItem, Int) -> Unit,
    private val onPriceEditClick: (PurchaseItem, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit,
    private val onQuantityClick: (PurchaseItem, Int) -> Unit,
) : RecyclerView.Adapter<PurchaseAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPurchaseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PurchaseItem) {
            binding.apply {
                tvNombreProducto.text = item.nombre
                tvStockActual.text = "Stock: ${item.stockActual}"
                tvCantidad.text = item.cantidad.toString()

                // Formateo de precios
                tvPrecioCompraEditable.text = String.format(Locale.US, "S/ %.2f", item.precioCompra)
                val subtotal = item.cantidad * item.precioCompra
                tvSubtotalItem.text = String.format(Locale.US, "S/ %.2f", subtotal)

                // Eventos
                btnPlus.setOnClickListener { onIncrementClick(item, bindingAdapterPosition) }
                btnMinus.setOnClickListener { onDecrementClick(item, bindingAdapterPosition) }

                // Clic en el precio para editarlo
                tvPrecioCompraEditable.setOnClickListener {
                    onPriceEditClick(item, bindingAdapterPosition)
                }
                tvCantidad.setOnClickListener {
                    onQuantityClick(item, bindingAdapterPosition)
                }
                // Long click para eliminar
                root.setOnLongClickListener {
                    onDeleteClick(bindingAdapterPosition)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPurchaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(purchaseList[position])
    }

    override fun getItemCount(): Int = purchaseList.size

    fun updateList(newList: List<PurchaseItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = purchaseList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return purchaseList[oldPos].idProducto == newList[newPos].idProducto
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return purchaseList[oldPos].cantidad == newList[newPos].cantidad &&
                        purchaseList[oldPos].precioCompra == newList[newPos].precioCompra
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        purchaseList.clear()
        purchaseList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}