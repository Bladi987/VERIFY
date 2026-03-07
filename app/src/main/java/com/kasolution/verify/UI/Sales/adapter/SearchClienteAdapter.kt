package com.kasolution.verify.UI.Sales.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.kasolution.verify.domain.clients.model.Client
import com.kasolution.verify.databinding.ItemClienteSuggestionBinding

class SearchClienteAdapter(context: Context, private val fullList: List<Client>) :
    ArrayAdapter<Client>(context, 0, fullList) {

    private var mFilteredList: List<Client> = fullList

    override fun getCount(): Int = mFilteredList.size
    override fun getItem(position: Int): Client = mFilteredList[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemClienteSuggestionBinding
        val view: View

        if (convertView == null) {
            binding = ItemClienteSuggestionBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemClienteSuggestionBinding
        }

        val cliente = getItem(position)

        // Asignación de datos
        binding.tvClienteNombre.text = cliente.nombre
        binding.tvClienteDocumento.text = "Doc: ${cliente.dniRuc}" // O el campo que uses (ruc, dni, etc)

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
                        // Filtramos por Nombre o por Documento
                        it.nombre.lowercase().contains(query) ||
                                it.dniRuc.contains(query)
                    }
                    results.values = filtered
                    results.count = filtered.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                mFilteredList = results?.values as? List<Client> ?: listOf()
                if (results?.count ?: 0 > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            // Lo que se verá en el buscador después de seleccionar
            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as Client).nombre
            }
        }
    }
}