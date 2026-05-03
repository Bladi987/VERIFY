package com.kasolution.verify.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object TicketManager {

    /**
     * Genera un PDF optimizado con escala y márgenes para ticket de 80mm
     */
    fun generateTicketPdf(context: Context, scrollView: NestedScrollView, fileName: String): File? {
        val view = scrollView.getChildAt(0)

        // Configuración de escala para evitar letras gigantes
        val targetWidth = 450 // Ancho estándar de puntos para ticket
        val scale = targetWidth.toFloat() / view.width.toFloat()
        val targetHeight = (view.height * scale).toInt()

        val document = PdfDocument()
        // Añadimos 40 puntos extra para márgenes físicos (aire alrededor del texto)
        val pageInfo = PdfDocument.PageInfo.Builder(targetWidth + 40, targetHeight + 40, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        canvas.translate(20f, 20f) // Margen de 20 puntos
        canvas.scale(scale, scale)

        view.draw(canvas)
        document.finishPage(page)

        return try {
            val file = File(context.cacheDir, "$fileName.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareTicket(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar por:"))
    }

    /**
     * Centraliza la generación de QR
     */
    fun generateQRCode(text: String, size: Int = 400): Bitmap? {
        return try {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }

    /**
     * Centraliza el monto en letras (Soles Peruanos)
     */
    fun formatAmountToLetters(monto: Double): String {
        val entero = monto.toLong()
        val centavos = ((monto - entero) * 100).toInt()
        val letras = NumberToLetterConverter.convert(entero) // Tu clase existente
        return "SON: $letras CON ${String.format(Locale.US, "%02d", centavos)}/100 SOLES"
    }
}