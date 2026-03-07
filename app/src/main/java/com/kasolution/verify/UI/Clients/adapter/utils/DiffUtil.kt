package com.kasolution.verify.UI.Clients.adapter.utils

import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.domain.clients.model.Client

class ClienteDiffCallback(
    private val oldList: List<Client>,
    private val newList: List<Client>
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