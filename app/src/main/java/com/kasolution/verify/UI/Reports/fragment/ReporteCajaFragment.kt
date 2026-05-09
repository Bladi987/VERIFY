package com.kasolution.verify.UI.Reports.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.kasolution.verify.UI.Reports.adapter.MovimientosCajaAdapter
import com.kasolution.verify.UI.Reports.viewModel.ReportesViewModel
import com.kasolution.verify.databinding.FragmentReporteCajaBinding
import com.kasolution.verify.domain.reports.model.ReporteCajaMovimiento

class ReporteCajaFragment : Fragment() {

    private var _binding: FragmentReporteCajaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportesViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReporteCajaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.cajaMovimientos.observe(viewLifecycleOwner) { movimientos ->
            // Si la lista es nula o vacía (como cuando limpiamos en el ViewModel), resetear gráfico
            if (movimientos.isNullOrEmpty()) {
                updateBalanceChart(0f, 0f)
                binding.tvTotalIngresosCaja.text = "S/ 0.00"
                binding.tvTotalEgresosCaja.text = "S/ 0.00"
                setupRecyclerView(emptyList())
                return@observe
            }

            // LÓGICA FLEXIBLE:
            val totalEgresos = movimientos.filter { it.tipo.equals("EGRESO", true) }.sumOf { Math.abs(it.monto) }
            val totalIngresos = movimientos.filter { !it.tipo.equals("EGRESO", true) }.sumOf { Math.abs(it.monto) }

            binding.tvTotalIngresosCaja.text = "S/ %.2f".format(totalIngresos)
            binding.tvTotalEgresosCaja.text = "S/ %.2f".format(totalEgresos)

            updateBalanceChart(totalIngresos.toFloat(), totalEgresos.toFloat())
            setupRecyclerView(movimientos)
        }
    }

    private fun updateBalanceChart(ingresos: Float, egresos: Float) {
        val entries = mutableListOf<BarEntry>()
        entries.add(BarEntry(0f, floatArrayOf(ingresos, egresos)))

        val dataSet = BarDataSet(entries, "Balance (Ingresos vs Egresos)").apply {
            colors = listOf(Color.parseColor("#2E7D32"), Color.parseColor("#C62828"))
            stackLabels = arrayOf("Ingresos", "Egresos")
            setDrawValues(true)
            valueTextColor = Color.BLACK // Cambiado a negro para mejor visibilidad si el fondo es claro
            valueTextSize = 12f
        }

        binding.chartBalanceCaja.apply {
            data = BarData(dataSet)
            setDrawGridBackground(false)
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            animateY(1000) // Animación para que se vea genial al cargar
            invalidate()
        }
    }

    private fun setupRecyclerView(lista: List<ReporteCajaMovimiento>) {
        binding.rvMovimientosCaja.apply {
            layoutManager = LinearLayoutManager(context)
            // Vinculamos el adaptador que creamos en el paso anterior
            adapter = MovimientosCajaAdapter(lista)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}