package com.kasolution.verify.UI.Sales.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.UI.Sales.model.CartItem
import com.kasolution.verify.databinding.ItemVentaProductoBinding
import java.util.Locale

class CartAdapter(
    private var cartList: MutableList<CartItem>,
    private val onIncrementClick: (CartItem, Int) -> Unit,
    private val onDecrementClick: (CartItem, Int) -> Unit,
    private val onPriceEditClick: (CartItem, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit,
    private val onQuantityClick: (CartItem, Int) -> Unit,
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemVentaProductoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Quitamos 'position' de los parámetros de bind
        fun bind(item: CartItem) {
            // 1. Asignación de datos
            binding.tvNombreProducto.text = item.producto.nombre
            binding.tvPrecioUnitarioVenta.text = String.format(Locale.US, "S/ %.2f", item.producto.precioVenta)
            binding.tvCantidad.text = item.cantidad.toString()
            binding.tvSubtotalVenta.text = String.format(Locale.US, "S/ %.2f", item.subtotal)
            binding.tvCodBar.text = "Codigo: ${item.producto.codigo}"


            // 2. Eventos de botones usando bindingAdapterPosition
            // Esto garantiza que el clic siempre use el índice real actual
            binding.btnMas.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onIncrementClick(item, currentPos)
                }
            }

            binding.btnMenos.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDecrementClick(item, currentPos)
                }
            }
            binding.tvPrecioUnitarioVenta.setOnClickListener {
                onPriceEditClick(item, bindingAdapterPosition)
            }
            binding.tvCantidad.setOnClickListener {
                onQuantityClick(item, bindingAdapterPosition)
            }

            // 3. Evento de eliminación (LongClick)
            binding.root.setOnLongClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteClick(currentPos)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVentaProductoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Pasamos solo el objeto, la posición la calculará el ViewHolder internamente
        holder.bind(cartList[position])
    }

    override fun getItemCount(): Int = cartList.size

    fun updateList(newList: List<CartItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = cartList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return cartList[oldPos].producto.id == newList[newPos].producto.id
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return cartList[oldPos].cantidad == newList[newPos].cantidad &&
                        cartList[oldPos].producto.precioVenta == newList[newPos].producto.precioVenta
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        cartList.clear()
        cartList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}