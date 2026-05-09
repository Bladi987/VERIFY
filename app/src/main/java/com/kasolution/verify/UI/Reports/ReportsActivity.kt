package com.kasolution.verify.UI.Reports

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayoutMediator
import com.kasolution.verify.R
import com.kasolution.verify.UI.Reports.adapter.ReportesPagerAdapter
import com.kasolution.verify.UI.Reports.viewModel.ReportesViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityReportsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {
    private val TAG = "ReportsActivity"
    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportesViewModel by viewModels {
        AppProvider.provideReportesViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViewPager()
        setupListeners()
        setupObservers()
        cargarDatosPredeterminados()
    }

    private fun setupViewPager() {
        val adapter = ReportesPagerAdapter(this)
        binding.viewPagerReportes.adapter = adapter

        // Vinculamos Tabs con ViewPager
        val tabTitles = arrayOf("Finanzas", "Inventario", "Caja")
        TabLayoutMediator(binding.tabLayoutReportes, binding.viewPagerReportes) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun setupListeners() {
        binding.btnSelectRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.toolbarReportes.setNavigationOnClickListener { onBackPressed() }
        binding.btnExportReport.setOnClickListener { compartirResumen() }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun compartirResumen() {
        val period = binding.btnSelectRange.text.toString()
        // Obtenemos los datos actuales de los LiveData del ViewModel
        val ventas = viewModel.ventasUtilidad.value?.sumOf { it.ingresos_totales } ?: 0.0
        val utilidad = viewModel.ventasUtilidad.value?.sumOf { it.utilidad_neta } ?: 0.0
        val periodo = binding.btnSelectRange.text.toString()

        val mensaje = """
        📊 *REPORTE DE NEGOCIO*
        📅 Periodo: $period
        
        💰 Ventas Totales: S/ ${"%.2f".format(ventas)}
        📈 Utilidad Neta: S/ ${"%.2f".format(utilidad)}
        
        Generado desde Verify App
    """.trimIndent()

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }
        // Verificamos que haya algo que compartir para evitar crashes
        if (ventas > 0) {
            startActivity(Intent.createChooser(intent, "Compartir reporte vía:"))
        } else {
            ToastHelper.clasicCustomToast(binding.root, "No hay datos suficientes para compartir", false)
        }
    }


    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar Periodo")
            .build()

        picker.show(supportFragmentManager, "range_picker")

        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val startDate = sdf.format(selection.first)
            val endDate = sdf.format(selection.second)
            binding.btnSelectRange.text = "$startDate a $endDate"
            viewModel.loadAllReportes(startDate, endDate)
        }
    }
    private fun cargarDatosPredeterminados() {
        val (inicio, fin) = obtenerRangoMesActual()

        // 1. Actualizamos visualmente el botón para que el usuario sepa qué fechas se están cargando
        binding.btnSelectRange.text = "$inicio a $fin"

        // 2. Disparamos la petición al ViewModel
        viewModel.loadAllReportes(inicio, fin)
    }
    private fun obtenerRangoMesActual(): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendario = java.util.Calendar.getInstance()

        // Fecha Fin: Hoy (08/05/2026)
        val fechaFin = sdf.format(calendario.time)

        // Fecha Inicio: Ajustamos al día 1 del mes actual (01/05/2026)
        calendario.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val fechaInicio = sdf.format(calendario.time)

        return Pair(fechaInicio, fechaFin)
    }
}