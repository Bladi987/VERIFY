package com.kasolution.verify.UI.Sales.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.kasolution.verify.domain.Inventory.model.Product
import com.kasolution.verify.databinding.ItemSearchSuggestionBinding

class SearchProductAdapter(context: Context, private val fullList: List<Product>) :
    ArrayAdapter<Product>(context, 0, fullList) {

    private var mFilteredList: List<Product> = fullList

    override fun getCount(): Int = mFilteredList.size
    override fun getItem(position: Int): Product = mFilteredList[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Declaramos el binding
        val binding: ItemSearchSuggestionBinding
        val view: View

        if (convertView == null) {
            // Si la vista es nueva, inflamos usando el binding
            binding = ItemSearchSuggestionBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            view = binding.root
            // Guardamos el binding en el tag para reciclarlo
            view.tag = binding
        } else {
            // Si la vista se recicla, recuperamos el binding del tag
            view = convertView
            binding = view.tag as ItemSearchSuggestionBinding
        }

        val product = getItem(position)

        // Asignamos datos directamente a través del binding
        binding.tvSearchNombre.text = product.nombre
        binding.tvSearchCodigo.text = "Cód: ${product.codigo ?: "---"}"
        binding.tvSearchPrecio.text = "S/ ${String.format("%.2f", product.precioVenta)}"

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrEmpty()) {
                    results.values = fullList
                    results.count = fullList.size
                } else {
                    val query = constraint.toString().lowercase().trim()
                    val filtered = fullList.filter {
                        it.nombre.lowercase().contains(query) ||
                                (it.codigo?.lowercase()?.contains(query) ?: false)
                    }
                    results.values = filtered
                    results.count = filtered.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                mFilteredList = results?.values as? List<Product> ?: listOf()
                if (results?.count ?: 0 > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as Product).nombre
            }
        }
    }
}