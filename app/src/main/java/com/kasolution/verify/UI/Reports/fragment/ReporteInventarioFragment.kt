package com.kasolution.verify.UI.Reports.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.UI.Reports.adapter.RankingAdapter
import com.kasolution.verify.UI.Reports.adapter.StockCriticoAdapter
import com.kasolution.verify.UI.Reports.viewModel.ReportesViewModel
import com.kasolution.verify.databinding.FragmentReporteInventarioBinding
import kotlin.getValue

class ReporteInventarioFragment : Fragment() {
private val TAG = "ReporteInventarioFragment"
    private var _binding: FragmentReporteInventarioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReporteInventarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardValoritation()
        setupObservers()
    }

    private fun setupCardValoritation() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val halfScreenWidth = screenWidth / 2

// Asignar a cada contenedor
        binding.itemCosto.layoutParams.width = halfScreenWidth
        binding.itemVenta.layoutParams.width = halfScreenWidth
        binding.itemGanancia.layoutParams.width = halfScreenWidth
    }

    private fun setupObservers() {
        viewModel.inventarioResumen.observe(viewLifecycleOwner) { data ->
            // 1. Usamos el operador ?. para que si 'data' es null, no intente acceder a las propiedades
            data?.let {
                binding.tvTotalCosto.text = "S/ %.2f".format(it.capital_invertido)
                binding.tvTotalVentaPotencial.text = "S/ %.2f".format(it.valor_venta_estimado)
                binding.tvGanacia.text = "S/ %.2f".format(it.valor_venta_estimado - it.capital_invertido)


                // 2. Configuramos el LayoutManager (solo una vez es necesario)
                if (binding.rvStockCritico.layoutManager == null) {
                    binding.rvStockCritico.layoutManager = LinearLayoutManager(requireContext())
                }

                // 3. LA SOLUCIÓN AL CRASH: Si la lista es null, enviamos una lista vacía (emptyList)
                val listaSegura = it.lista_productos_criticos ?: emptyList()
                binding.rvStockCritico.adapter = StockCriticoAdapter(listaSegura)
            }
        }
        viewModel.topProductos.observe(viewLifecycleOwner) { lista ->
            lista?.let {
                if (binding.rvRankingProductos.layoutManager == null) {
                    binding.rvRankingProductos.layoutManager = LinearLayoutManager(requireContext())
                }
                val adapterRanking = RankingAdapter(it)
                binding.rvRankingProductos.adapter = adapterRanking
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}