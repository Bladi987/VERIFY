package com.kasolution.verify.UI.Reports.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.*
import com.kasolution.verify.UI.Reports.viewModel.ReportesViewModel
import com.kasolution.verify.databinding.FragmentReporteFinanzasBinding
import com.kasolution.verify.domain.reports.model.ReporteVenta
import kotlin.getValue

class ReporteFinanzasFragment : Fragment() {

    private var _binding: FragmentReporteFinanzasBinding? = null
    private val binding get() = _binding!!

    // Compartimos el ViewModel de la Activity
    private val viewModel: ReportesViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReporteFinanzasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        // Observamos Ventas y Utilidad
        viewModel.ventasUtilidad.observe(viewLifecycleOwner) { lista ->
            updateMainChart(lista)
            updateKPIs(lista)
        }

        // Observamos Métodos de Pago
        viewModel.metodosPago.observe(viewLifecycleOwner) { lista ->
            updatePieChart(lista)
        }
    }

    private fun updateKPIs(lista: List<ReporteVenta>) {
        val totalVentas = lista.sumOf { it.ingresos_totales }
        val totalUtilidad = lista.sumOf { it.utilidad_neta }

        binding.tvTotalVentas.text = "S/ %.2f".format(totalVentas)
        binding.tvTotalUtilidad.text = "S/ %.2f".format(totalUtilidad)
    }

    private fun updateMainChart(lista: List<ReporteVenta>) {
        val entriesVentas = mutableListOf<BarEntry>()
        val entriesUtilidad = mutableListOf<Entry>()

        lista.forEachIndexed { index, item ->
            entriesVentas.add(BarEntry(index.toFloat(), item.ingresos_totales.toFloat()))
            entriesUtilidad.add(Entry(index.toFloat(), item.utilidad_neta.toFloat()))
        }

        val barDataSet = BarDataSet(entriesVentas, "Ventas").apply {
            color = Color.parseColor("#448AFF")
            setDrawValues(false)
        }

        val lineDataSet = LineDataSet(entriesUtilidad, "Utilidad").apply {
            color = Color.parseColor("#00C853")
            lineWidth = 3f
            setCircleColor(Color.parseColor("#00C853"))
            setDrawValues(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.combinedChart.data = CombinedData().apply {
            setData(BarData(barDataSet))
            setData(LineData(lineDataSet))
        }
        binding.combinedChart.invalidate()
    }

    private fun updatePieChart(lista: List<com.kasolution.verify.domain.reports.model.ReporteMetodoPago>) {
        val entries = lista.map {
            PieEntry(it.total.toFloat(), it.metodo)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.LTGRAY)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.pieChartPagos.data = PieData(dataSet)
        binding.pieChartPagos.description.isEnabled = false
        binding.pieChartPagos.centerText = "Métodos"
        binding.pieChartPagos.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}