package com.kasolution.verify.UI.Reports.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.kasolution.verify.UI.Reports.viewModel.ReportesViewModel
import com.kasolution.verify.databinding.FragmentReporteFinanzasBinding
import com.kasolution.verify.domain.reports.model.ReporteMetodoPago
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

    private fun updatePieChart(lista: List<ReporteMetodoPago>) {
        val entries = lista.map {
            PieEntry(it.total.toFloat(), it.metodo.uppercase())
        }

        val coloresPersonalizados = lista.map {
            when (it.metodo.uppercase()) {
                "EFECTIVO" -> Color.parseColor("#4CAF50")
                "YAPE"     -> Color.parseColor("#9822A7")
                "PLIN"     -> Color.parseColor("#0DD5D3")
                "TARJETA"  -> Color.parseColor("#1976D2")
                else       -> Color.parseColor("#757575")
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = coloresPersonalizados
            sliceSpace = 3f

            // Mantenemos los valores fuera para que no ensucien el gráfico
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.2f
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.pieChartPagos.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "S/ %.2f".format(value)
                })
            }

            // --- CONFIGURACIÓN PARA GRÁFICO GRANDE ---
            description.isEnabled = false
            centerText = "Ingresos"
            setHoleRadius(50f)

            // Eliminamos offsets laterales excesivos para que el círculo crezca
            setExtraOffsets(20f, 0f, 20f, 0f)

            // --- LEYENDA EN LA PARTE INFERIOR ---
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM // Abajo
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER // Centrado
                orientation = Legend.LegendOrientation.HORIZONTAL // Horizontal para que no ocupe mucho alto
                setDrawInside(false)
                isWordWrapEnabled = true // Si hay muchos métodos, salta de línea

                xEntrySpace = 15f // Espacio entre items de la leyenda
                yEntrySpace = 5f
                textSize = 12f
                form = Legend.LegendForm.CIRCLE // Formato de la leyenda
            }

            // Esto es clave: permite que el gráfico use el espacio que antes ocupaba la leyenda lateral
            minOffset = 0f

            animateY(800)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}