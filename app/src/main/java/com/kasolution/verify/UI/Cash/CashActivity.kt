package com.kasolution.verify.UI.Cash

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasolution.verify.R
import com.kasolution.verify.UI.Cash.adapter.CashAdapter
import com.kasolution.verify.UI.Cash.fragment.CashCloseSheet
import com.kasolution.verify.UI.Cash.viewModel.CashViewModel
import com.kasolution.verify.core.AppProvider
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ProgressHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.core.utils.setupCurrencyFormatting
import com.kasolution.verify.databinding.ActivityCashBinding
import com.kasolution.verify.databinding.DialogAddMovementBinding
import com.kasolution.verify.databinding.DialogChangePriceBinding
import com.kasolution.verify.databinding.DialogOpenCashierBinding
import com.kasolution.verify.databinding.DialogSupervisorAuthBinding

class CashActivity : AppCompatActivity() {
    private val tag = "CashActivity"
    private lateinit var binding: ActivityCashBinding
    private lateinit var adapter: CashAdapter
    private val viewModel: CashViewModel by viewModels {
        AppProvider.provideCashViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initRecycler()
        setupObservers()
        setupListeners()
    }

    private fun initRecycler() {
        adapter = CashAdapter(mutableListOf()) { movement ->
            // Click opcional para ver detalle del movimiento
        }
        binding.rvCashHistory.apply {
            layoutManager = LinearLayoutManager(this@CashActivity)
            adapter = this@CashActivity.adapter
        }
    }

    private fun setupObservers() {
        // 1. Observar Sesión Activa (Cambia la UI completa)
        viewModel.activeSessionId.observe(this) { idSesion ->
            val isCashOpen = idSesion != null && idSesion > 0
            Log.d("CashActivity", "Caja abierta: $isCashOpen (ID: $idSesion)")
            binding.layoutOpenCash.isVisible = !isCashOpen
            binding.layoutCashDashboard.isVisible = isCashOpen

            if (isCashOpen) {
                if (viewModel.isAuditorMode()) {
                    binding.fabAddMovement.hide() // No puede agregar gastos
                    binding.btnCloseCash.isVisible = false // No puede cerrar la caja de otro
                    ToastHelper.clasicCustomToast(
                        binding.root,
                        "Modo Auditoría: Solo Lectura",
                        true
                    )
                } else {
                    binding.fabAddMovement.show()
                    binding.btnCloseCash.isVisible = true
                }
                viewModel.loadHistory()
            } else {
                binding.fabAddMovement.hide()

                // Si el ADMIN llega y la caja está cerrada, no debería poder abrirla
                if (viewModel.isAuditorMode()) {
                    binding.btnOpenCash.isEnabled = false
                    binding.btnOpenCash.text = "CAJA CERRADA (AUDITOR)"
                    binding.etInitialAmount.isEnabled = false
                }
            }
        }

        // 2. Observar Historial de Movimientos
        viewModel.cashHistory.observe(this) { lista ->
            Log.d("CashActivity", "Historial de movimientos recibido: $lista")
            adapter.updateList(lista ?: emptyList())
            //updateSummaryCards(lista ?: emptyList())
            toggleEmptyState(lista.isNullOrEmpty())
        }

        // 3. Loading (Usando tu ProgressHelper)
        viewModel.isLoading.observe(this) { loading ->
            if (loading) ProgressHelper.showProgress(this, "Procesando...")
            else ProgressHelper.hideProgress()
        }

        // 4. Errores (Usando tu ToastHelper)
        viewModel.exception.observe(this) { error ->
            if (error.isNotEmpty()) {
                ToastHelper.clasicCustomToast(binding.root, error, false)
                viewModel.resetOperationStatus()
            }
        }

        // 5. Éxito de Operaciones
        viewModel.operationSuccess.observe(this) { action ->
            Log.d(tag, "Operación exitosa: $action")
            if (action.isEmpty()) return@observe
            var msg=""
            when (action) {
                "CASH_OPEN"-> msg="¡Caja abierta correctamente!"
                "CASH_ADD_MOVEMENT"->msg="Movimiento registrado"
                "CASH_CLOSE_SUCCESS" -> {
                }

            }
            if (msg.isNotEmpty()) ToastHelper.clasicCustomToast(binding.root, msg, true)
        }
        viewModel.saldoInicial.observe(this) { inicial ->
            binding.rowInitial.tvLabel.text = "Saldo Inicial"
            binding.rowInitial.tvValue.text = "S/ ${String.format("%.2f", inicial)}"
        }

        viewModel.totalVentas.observe(this) { ventas ->
            binding.rowSales.tvLabel.text = "Ventas Totales"
            binding.rowSales.tvValue.text = "S/ ${String.format("%.2f", ventas)}"
        }

        viewModel.totalEgresos.observe(this) { egresos ->
            binding.rowPurchases.tvLabel.text = "Gastos / Egresos"
            binding.rowPurchases.tvValue.text = "S/ ${String.format("%.2f", egresos)}"
        }

        viewModel.totalAjustes.observe(this) { monto ->
            binding.rowMovements.tvLabel.text = "Otros Movimientos"
            // Mostramos si es positivo o negativo para claridad
            val prefix = if (monto >= 0) "+" else ""
            binding.rowMovements.tvValue.text = "$prefix S/ ${String.format("%.2f", monto)}"
        }
        viewModel.saldoEsperado.observe(this) { esperado ->
            binding.tvExpectedBalance.text = "S/ ${String.format("%.2f", esperado)}"
        }

        // 6. Reporte de Cierre (Arqueo Final)
        viewModel.closeReport.observe(this) { report ->

            report?.let {
//                Log.d(tag, "Reporte de cierre : $it")
//                showClosingSummaryDialog(it)
                val ticketSheet = CashCloseSheet()
                ticketSheet.show(supportFragmentManager, "CashCloseSheet")
//                viewModel.resetOperationStatus()
            }
        }
    }

    private fun setupListeners() {
        // Botón para cerrar caja (Inicia proceso de Arqueo)
        binding.btnCloseCash.setOnClickListener {
            showArqueoDialog()
        }

        // FAB: Movimiento manual (Ingreso/Egreso)
        binding.fabAddMovement.setOnClickListener {
            // Aquí llamarías a un DialogFragment o Dialog similar a ProductForm
            showManualMovementDialog()
        }
        binding.btnOpenCash.setOnClickListener {
            showOpenCashierDialog()
        }
    }


    private fun showOpenCashierDialog() {
        val binding = DialogOpenCashierBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, binding.root)

        // Aplicamos tu formateo automático
        binding.etInitialAmount.setupCurrencyFormatting()

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        binding.btnConfirm.setOnClickListener {
            val monto = binding.etInitialAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (binding.etInitialAmount.text.toString().isEmpty()) {
                binding.tilInitialAmount.error = "Ingrese un monto valido"
            } else {
                if (viewModel.needsSupervisorToOpen()) {
                    showSupervisorAuthDialog(monto)
                }else viewModel.openCash(monto)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    private fun showArqueoDialog() {
        val dialogBinding = DialogChangePriceBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, dialogBinding.root)
        val color: Int = R.color.blue_corporative_primary
        dialogBinding.tvDialogTitle.text = "Arqueo de Caja"
        dialogBinding.tvDialogMessage.text = "Por favor, cuente el dinero en efectivo y escriba el monto total:"
        dialogBinding.tilPrice.hint="Dinero físico en caja"
        dialogBinding.btnConfirm.text = "CERRAR CAJA"
        dialogBinding.etPrice.setupCurrencyFormatting()
        dialogBinding.btnConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, color))
        dialogBinding.tilPrice.boxStrokeColor = ContextCompat.getColor(this, color)
        dialogBinding.tilPrice.hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(this, color))
        dialogBinding.tilPrice.setPrefixTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, color)))
        dialogBinding.btnConfirm.setOnClickListener {
            if (dialogBinding.etPrice.text.toString().isNotEmpty()) {
                val monto = dialogBinding.etPrice.text.toString().toDouble()
                viewModel.closeCash(monto)
                dialog.dismiss()
            }else dialogBinding.tilPrice.error = "Ingrese un monto válido"
        }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()

    }

    private fun showManualMovementDialog() {
        val dialogBinding = DialogAddMovementBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, dialogBinding.root)
        dialogBinding.etMovementAmount.setupCurrencyFormatting()
        // Configurar el Dropdown
        val tipos = arrayOf("INGRESO", "EGRESO")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tipos)
        dialogBinding.actvMovementType.setAdapter(adapter)
        dialogBinding.actvMovementType.setText(tipos[0], false)
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnRegistrar.setOnClickListener {
            // 1. Resetear errores previos
            dialogBinding.tilMovementAmount.error = null
            dialogBinding.tilMovementReason.error = null
            // 2. Obtener valores
            val tipo = dialogBinding.actvMovementType.text.toString()
            val montoStr = dialogBinding.etMovementAmount.text.toString()
            val monto = montoStr.toDoubleOrNull()
            val motivo = dialogBinding.etMovementReason.text.toString().trim()
            var isValid = true
            // 3. Validar Monto
            if (monto == null || monto <= 0) {
                dialogBinding.tilMovementAmount.error = "Ingresa un monto válido mayor a 0"
                isValid = false
            }
            // 4. Validar Motivo
            if (motivo.isEmpty()) {
                dialogBinding.tilMovementReason.error = "El motivo es obligatorio"
                isValid = false
            }
            // 5. Acción final
            if (isValid) {
                viewModel.addMovement(tipo, monto!!, motivo)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvCashHistory.isVisible = false
        } else {
            binding.rvCashHistory.isVisible = true
        }
    }

    // Agrega este método a CashActivity
    private fun showSupervisorAuthDialog(montoInicial: Double) {
        // 1. Inflar el binding del diálogo
        val dialogBinding = DialogSupervisorAuthBinding.inflate(layoutInflater)
        val dialog = DialogHelper.createBaseDialog(this, dialogBinding.root)
        dialogBinding.btnAutorizar.setOnClickListener {
            dialogBinding.tilSupUser.error = null
            dialogBinding.tilSupPass.error = null
            var isValid = true
            val user = dialogBinding.etSupUser.text.toString().trim()
            val pass = dialogBinding.etSupPass.text.toString().trim()
            if (user.isEmpty()) {
                dialogBinding.tilSupUser.error = "El usuario es obligatorio"
                isValid = false
            }
            if (pass.isEmpty()) {
                dialogBinding.tilSupPass.error = "La contraseña es obligatoria"
                isValid = false
            }
            if (isValid) {
                viewModel.openCashAuthorized(montoInicial, user, pass)
                dialog.dismiss()
            }
        }
        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}