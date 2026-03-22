package com.kasolution.verify.UI.Purchase.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.kasolution.verify.domain.supplier.model.Supplier
import java.util.Locale

class SearchSupplierAdapter(
    context: Context,
    private val fullList: List<Supplier>
) : ArrayAdapter<Supplier>(context, 0, fullList) {

    private var filteredList: List<Supplier> = fullList

    override fun getCount(): Int = filteredList.size

    override fun getItem(position: Int): Supplier? = filteredList.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

        val supplier = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        // Mostramos el nombre y el teléfono como referencia secundaria
        textView.text = supplier?.let { "${it.nombre} (${it.telefono})" } ?: ""

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase(Locale.ROOT)?.trim()

                filteredList = if (query.isNullOrEmpty()) {
                    fullList
                } else {
                    fullList.filter {
                        // Filtramos por Nombre o por Dirección
                        it.nombre.lowercase(Locale.ROOT).contains(query) ||
                                it.direccion.lowercase(Locale.ROOT).contains(query)
                    }
                }

                results.values = filteredList
                results.count = filteredList.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null) {
                    // Importante: No llamar a notifyDataSetChanged si el conteo es 0
                    // para evitar parpadeos innecesarios en algunos dispositivos
                    notifyDataSetChanged()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                // Lo que se escribe en el AutoCompleteTextView al seleccionar
                return (resultValue as Supplier).nombre
            }
        }
    }
}