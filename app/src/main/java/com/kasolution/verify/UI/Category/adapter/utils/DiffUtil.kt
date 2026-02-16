package com.kasolution.verify.UI.Category.adapter.utils

import androidx.recyclerview.widget.DiffUtil
import com.kasolution.verify.UI.Category.model.Category

class CategoriesDiffCallback(
    private val oldList: List<Category>,
    private val newList: List<Category>
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