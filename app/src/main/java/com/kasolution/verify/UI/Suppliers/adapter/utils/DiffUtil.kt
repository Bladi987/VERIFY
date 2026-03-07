package com.kasolution.verify.UI.Suppliers.adapter.utils

import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.domain.supplier.model.Supplier


class SupplierDiffCallback(
    private val oldList: List<Supplier>,
    private val newList: List<Supplier>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    // Compara si son el mismo objeto (normalmente por ID)
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    // Compara si el contenido de los objetos es idéntico
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}