package com.kasolution.verify.UI.Dashboard.View.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasolution.verify.R
import com.kasolution.verify.UI.Dashboard.View.model.itemGridMenu
import com.kasolution.verify.databinding.ItemGridBinding

class gridMenuAdapter (
    private val listaRecibida: ArrayList<itemGridMenu>,
    private val OnClickListener: (itemGridMenu) -> Unit
) : RecyclerView.Adapter<gridMenuAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): gridMenuAdapter.ViewHolder {
        val layoutInflater =
            LayoutInflater.from(parent.context).inflate(R.layout.item_grid, parent, false)
        return ViewHolder(layoutInflater)
    }

    override fun getItemCount(): Int {
        return listaRecibida.size
    }

    override fun onBindViewHolder(holder: gridMenuAdapter.ViewHolder, position: Int) {
        val item = listaRecibida[position]
        holder.render(item, OnClickListener)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding = ItemGridBinding.bind(view)

        fun render(
            lista: itemGridMenu,
            OnClickListener: (itemGridMenu) -> Unit
        ) {
            itemView.setOnClickListener { OnClickListener(lista) }
            binding.moduleIcon.setImageResource(lista.icon!!)
            binding.moduleTitle.text = lista.name
        }
    }
}