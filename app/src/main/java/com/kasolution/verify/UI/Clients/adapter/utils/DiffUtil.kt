package com.kasolution.verify.UI.Clients.adapter.utils

import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.UI.Clients.model.Cliente

class ClienteDiffCallback(
    private val oldList: List<Cliente>,
    private val newList: List<Cliente>
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