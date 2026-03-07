package com.kasolution.verify.UI.Components.Scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.VibrationEffect
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.kasolution.verify.UI.Components.Scanner.viewModel.ScannerViewModelFactory
import com.kasolution.verify.core.utils.DialogHelper
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.data.network.SocketManager
import com.kasolution.verify.data.repository.InventoryRepository
import com.kasolution.verify.databinding.ActivityScannerBinding

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private var cameraControl: CameraControl? = null
    private lateinit var toneGenerator: ToneGenerator
    private var isMultiScan: Boolean = false
    private var totalEscaneados: Int = 0
    private var barcodeAnalyzer: BarcodeAnalyzer? = null
    private var currentCode: String = ""
    private var montoTotal: Double = 0.0

    private val viewModel: ScannerViewModel by viewModels {
        ScannerViewModelFactory(InventoryRepository(SocketManager.getInstance()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isMultiScan = intent.getBooleanExtra("MULTI_SCAN", false)
        binding.tvBadgeContador.visibility = if (isMultiScan) View.VISIBLE else View.GONE
        binding.cardTotal.visibility = if (isMultiScan) View.VISIBLE else View.GONE

        montoTotal = intent.getDoubleExtra("INITIAL_TOTAL", 0.0)
        totalEscaneados = intent.getIntExtra("INITIAL_COUNT", 0)

        if (isMultiScan) {
            binding.tvTotalAcumulado.text = "S/ ${String.format("%.2f", montoTotal)}"
            binding.tvBadgeContador.text = totalEscaneados.toString()
            binding.cardTotal.visibility = View.VISIBLE
        }

        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

        setupObservers()
        checkPermissionsAndStart()
    }

    private fun setupObservers() {
        viewModel.productFound.observe(this) { producto ->
            // Este observador SOLO se activará en Modo Ventas (MultiScan)
            if (isMultiScan) {
                val existe = producto != null

                // Feedback Visual y Sonoro según si existe el producto
                binding.scannerOverlay.flashResultColor(existe)
                playFeedbackSound(existe)
                vibrate(existe)


                if (existe) {
                    totalEscaneados++
                    montoTotal += producto!!.precioVenta
                    binding.tvBadgeContador.text = totalEscaneados.toString()

                    val totalFormateado = "S/ ${String.format("%.2f", montoTotal)}"
                    binding.tvTotalAcumulado.text = totalFormateado

                    showProductFeedback(
                        producto!!.nombre,
                        "S/ ${String.format("%.2f", producto.precioVenta)}"
                    )
                    binding.cardTotal.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100)
                        .withEndAction {
                            binding.cardTotal.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                                .start()
                        }.start()
                } else {
                    showProductFeedback("No encontrado", "Código: ${currentCode}")
                }

                // Reanudar escaneo
                binding.root.postDelayed({
                    barcodeAnalyzer?.resumeScanning()
                }, 1500)
            }
        }
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // 2. Analysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            barcodeAnalyzer = BarcodeAnalyzer(binding.scannerOverlay, binding.previewView) { code ->
                runOnUiThread { procesarCodigo(code) }
            }

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), barcodeAnalyzer!!)

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )

                cameraControl = camera.cameraControl
                setupCameraControls(camera)

            } catch (ex: Exception) {
                Log.e("Scanner", "Error al vincular cámara", ex)
            }
        }, ContextCompat.getMainExecutor(this))
        binding.previewView.setOnTouchListener { _, event ->
            val factory = binding.previewView.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl?.startFocusAndMetering(action)
            true
        }
    }

    private fun setupCameraControls(camera: Camera) {
        binding.btnFlash.setOnClickListener {
            val isFlashOn = camera.cameraInfo.torchState.value == TorchState.ON
            cameraControl?.enableTorch(!isFlashOn)
        }

        camera.cameraInfo.torchState.observe(this) { state ->
            val isFlashOn = state == TorchState.ON

            // Cambiamos el color: Amarillo si está encendido, Blanco si está apagado
            val colorIcono = if (isFlashOn) {
                ContextCompat.getColor(this, android.R.color.holo_orange_light) // O Color.YELLOW
            } else {
                ContextCompat.getColor(this, android.R.color.white)
            }

            // Aplicamos el tinte al ImageButton
            binding.btnFlash.setColorFilter(colorIcono)

        }
        binding.btnCancelar.setOnClickListener { finish() }
        binding.btnReset.setOnClickListener {
            DialogHelper.showConfirmation(
                context = this,
                title = "¿Vaciar Carrito?",
                message = "Se eliminarán todos los productos agregados. ¿Deseas continuar?",
                onConfirm = {
                    // 1. Resetear variables
                    montoTotal = 0.0
                    totalEscaneados = 0

                    // 2. Limpiar UI
                    binding.tvTotalAcumulado.text = "S/ 0.00"
                    binding.tvBadgeContador.text = "0"

                    // 3. Feedback táctil
                    vibrate(true)
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)

                    // 4. NOTIFICACIÓN OBLIGATORIA al sistema de ventas
                    val intentReset = Intent("ACTION_CLEAR_CART").apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(intentReset)
                })

        }
    }

    private fun procesarCodigo(code: String) {
        currentCode = code
        if (isMultiScan) {
            // Comunicación con SalesActivity
            sendBroadcast(Intent("ACTION_PRODUCT_SCANNED").apply {
                putExtra("SCAN_RESULT_CODE", code)
                setPackage(packageName)
            })

            viewModel.findProductByCode(code)

            // Reanudar escaneo tras pausa para feedback
            binding.root.postDelayed({ barcodeAnalyzer?.resumeScanning() }, 1500)
        } else {
            binding.scannerOverlay.flashResultColor(true) // Siempre verde en modo simple
            playFeedbackSound(true) // Beep normal
            vibrate(true) // Vibración normal


            // Pequeña pausa para que se vea el flash antes de cerrar
            binding.root.postDelayed({
                enviarResultadoFinal(code)
            }, 500)
            enviarResultadoFinal(code)
        }

    }

    private fun playFeedbackSound(isSuccess: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean("beep_enabled", true)) return

        if (isSuccess) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150) // Beep éxito
        } else {
            toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 400) // Sonido error
        }
    }

    private fun vibrate(isSuccess: Boolean) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (isSuccess) {
                // Vibración corta (50ms)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    vibrator.vibrate(50)
                }
            } else {
                // Vibración doble para Error
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 50, 100, 50)
                    val amplitudes = intArrayOf(
                        0,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                        0,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(longArrayOf(0, 50, 100, 50), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Error al vibrar", e)
        }
    }

    private fun showProductFeedback(nombre: String, precio: String) {
        binding.layoutFeedback.apply {
            tvProductName.text = nombre
            tvProductPrice.text = precio

            val card = root as com.google.android.material.card.MaterialCardView
            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.animate().alpha(1f).setDuration(300).withEndAction {
                card.postDelayed({
                    card.animate().alpha(0f).setDuration(300).withEndAction {
                        card.visibility = View.GONE
                    }.start()
                }, 2000)
            }.start()
        }
    }

    private fun enviarResultadoFinal(code: String) {
        setResult(RESULT_OK, Intent().apply { putExtra("SCAN_RESULT", code) })
        finish()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera() else finish()
        }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }
}