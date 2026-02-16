package com.kasolution.verify.UI.Components.Scanner // Ajusta a tu paquete real

import android.annotation.SuppressLint
import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val scannerOverlayView: ScannerOverlayView,
    private val previewView: PreviewView,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var isScanning = true // Control para evitar lecturas duplicadas inmediatas

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        // 1. AJUSTE DE ROTACIÓN: CameraX entrega la imagen rotada internamente.
        // Si la rotación es 90 o 270, invertimos ancho y alto para que el mapeo sea correcto.
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270

        val fullImageWidth = if (isRotated) mediaImage.height else mediaImage.width
        val fullImageHeight = if (isRotated) mediaImage.width else mediaImage.height

        // 2. OBTENER RECUADRO DEL DISEÑO (Coordenadas de la pantalla)
        val scanBoxRectOnView = scannerOverlayView.getScanBoxRect()

        // 3. MATRIZ DE MAPEO: Traduce de "Píxeles de Pantalla" a "Píxeles de Imagen de Cámara"
        val matrix = Matrix()
        val sourceRect = RectF(0f, 0f, previewView.width.toFloat(), previewView.height.toFloat())
        val destinationRect = RectF(0f, 0f, fullImageWidth.toFloat(), fullImageHeight.toFloat())

        // FILL asegura que el mapeo cubra toda la resolución de entrada
        matrix.setRectToRect(sourceRect, destinationRect, Matrix.ScaleToFit.FILL)

        val mappedScanRectF = RectF()
        matrix.mapRect(mappedScanRectF, scanBoxRectOnView)

        val mappedScanRect = Rect(
            mappedScanRectF.left.toInt(),
            mappedScanRectF.top.toInt(),
            mappedScanRectF.right.toInt(),
            mappedScanRectF.bottom.toInt()
        )

        // 4. PROCESAR CON ML KIT
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)


        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    val boundingBox = barcode.boundingBox

                    if (rawValue != null && boundingBox != null) {
                        // VALIDACIÓN: ¿El centro del código está dentro de nuestro visor azul?
                        if (mappedScanRect.contains(boundingBox.centerX(), boundingBox.centerY())) {
                            isScanning = false // Pausamos el escaneo para procesar
                            onBarcodeDetected(rawValue)

                            // Opcional: Reactivar después de 2 segundos para permitir el siguiente producto
                            // previewView.postDelayed({ isScanning = true }, 2000)
                            break
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()

            }
    }

    // Método para reactivar el escáner desde la Activity (ej. después de cerrar un diálogo)
    fun resumeScanning() {
        isScanning = true
    }
}