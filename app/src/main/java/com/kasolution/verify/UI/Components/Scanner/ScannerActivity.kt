package com.kasolution.verify.UI.Components.Scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.kasolution.verify.core.utils.ToastHelper
import com.kasolution.verify.databinding.ActivityScannerBinding

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private var cameraControl: CameraControl? = null
    private lateinit var toneGenerator: ToneGenerator
    private var isMultiScan: Boolean = false
    private var totalEscaneados: Int = 0

    // Guardamos una referencia al analizador por si necesitamos pausar/reanudar
    private var barcodeAnalyzer: BarcodeAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recibimos si es multi-scan desde el Intent que lanza esta actividad
        isMultiScan = intent.getBooleanExtra("MULTI_SCAN", false)
        binding.tvBadgeContador.visibility = if (isMultiScan) View.VISIBLE else View.GONE
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun setupButtons() {
        binding.btnCancelar.setOnClickListener {
            // Si es multiscan, quizás quieras devolver lo que llevas o simplemente cerrar
            finish()
        }

        // El click del Flash lo movimos dentro de startCamera para tener acceso al objeto camera
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            ToastHelper.showCustomToast(binding.root, "Permiso de cámara denegado", false)
            finish()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Configurar el flujo de Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // 2. Configurar el Analizador usando tu nueva clase BarcodeAnalyzer
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Instanciamos el analizador pasando el Overlay, el PreviewView y el Callback
            barcodeAnalyzer = BarcodeAnalyzer(
                binding.scannerOverlay,
                binding.previewView
            ) { code ->
                // Esta es la acción que ocurre cuando se detecta un código válido dentro del cuadro
//                enviarResultado(code)
                runOnUiThread {
                    procesarCodigo(code)
                }
            }


            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                barcodeAnalyzer!!
            )

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                cameraControl = camera.cameraControl

                // Configurar el botón de flash
                binding.btnFlash.setOnClickListener {
                    val isFlashOn = camera.cameraInfo.torchState.value == TorchState.ON
                    cameraControl?.enableTorch(!isFlashOn)
                }
                binding.btnCancelar.setOnClickListener {
                    finish()
                }

            } catch (ex: Exception) {
                Log.e("Scanner", "Error al iniciar cámara", ex)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun procesarCodigo(code: String) {

        binding.scannerOverlay.flashSuccessColor()
        // Sonido de confirmación
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("beep_enabled", true)) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }

        if (isMultiScan) {
            // --- MODO MULTI-SCAN ---
            totalEscaneados++
            binding.tvBadgeContador.text = totalEscaneados.toString()

            // Aquí es donde "simulamos" que el módulo de ventas nos da la info.
            // En un escenario real, aquí podrías hacer un callback o una petición rápida.
            showProductFeedback("Producto #$totalEscaneados", "Código: $code")
            // REACTIVACIÓN AUTOMÁTICA
            // Esperamos 1.5 segundos antes de permitir que el analizador vuelva a leer
            binding.root.postDelayed({
                barcodeAnalyzer?.resumeScanning()
            }, 1500)

        } else {
            // --- MODO SIMPLE ---
            enviarResultadoFinal(code)
        }
    }

    private fun showProductFeedback(nombre: String, precio: String) {
        binding.layoutFeedback.apply {
            // Ahora sí reconocerá tvProductName y tvProductPrice
            tvProductName.text = nombre
            tvProductPrice.text = precio

            // El root del include es el MaterialCardView
            val card = root as com.google.android.material.card.MaterialCardView

            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.animate().alpha(1f).setDuration(300).start()

            card.postDelayed({
                card.animate().alpha(0f).setDuration(300).withEndAction {
                    card.visibility = View.GONE
                }.start()
            }, 2000)
        }
    }

    private fun enviarResultadoFinal(code: String) {
        val data = Intent().apply { putExtra("SCAN_RESULT", code) }
        setResult(RESULT_OK, data)
        finish()
    }


    private fun enviarResultado(code: String) {
        // 1. Verificar si el sonido está activado en ajustes
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val beepEnabled = prefs.getBoolean("beep_enabled", true) // "beep_enabled" es la llave en tus ajustes

        if (beepEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }

        // Devolvemos el resultado y cerramos la actividad
        val data = Intent().apply {
            putExtra("SCAN_RESULT", code)
        }
        setResult(RESULT_OK, data)
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }
}