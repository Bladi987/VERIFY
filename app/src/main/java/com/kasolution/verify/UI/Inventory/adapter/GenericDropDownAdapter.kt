package com.kasolution.verify.UI.Inventory.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.kasolution.verify.R

class GenericDropDownAdapter<T>(
    context: Context,
    private val items: List<T>,
    private val itemToString: (T) -> String
) : ArrayAdapter<T>(context, R.layout.list_item_dropdown, items) {

    // Lo que se muestra en la lista desplegable
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = itemToString(items[position])
        return view
    }

    // Lo que se muestra en el campo de texto después de seleccionar
    override fun getItem(position: Int): T? = items[position]

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = items
                results.count = items.size
                return results
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
            override fun convertResultToString(resultValue: Any?): CharSequence {
                return itemToString(resultValue as T)
            }
        }
    }
}