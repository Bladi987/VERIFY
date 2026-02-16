package com.kasolution.verify.UI.Components.Scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.kasolution.verify.R

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colorAzul = ContextCompat.getColor(context, R.color.blue_corporative_primary)
    private val colorExito = Color.GREEN
    private val transparentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    // Configuración para el Láser usando el recurso XML
    private val laserDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.bg_laser_line)
    private var laserYOffset = 0f
    private var laserAnimator: ValueAnimator? = null
    private val laserHeight = 12 // Grosor visual de la línea láser

    init {
        // 1. Configuración del "agujero" transparente
        transparentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        // 2. Configuración de las esquinas (Azul Corporativo y Grueso)
        framePaint.apply {
            color = colorAzul
            style = Paint.Style.STROKE
            strokeWidth = 20f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // 3. Superposición oscura
        overlayPaint.color = Color.parseColor("#99000000")

        // Crucial para que PorterDuff.Mode.CLEAR y el dibujo de capas funcionen
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        // Iniciar la animación después de que la vista se haya renderizado
        post { startLaserAnimation() }
    }

    private fun startLaserAnimation() {
        // Calculamos el recorrido del láser basado en el 35% del alto total de la vista
        val totalFrameHeight = height * 0.35f

        laserAnimator = ValueAnimator.ofFloat(0f, totalFrameHeight).apply {
            duration = 2000 // 2 segundos por barrido
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                laserYOffset = it.animatedValue as Float
                invalidate() // Redibujar para mostrar el movimiento del láser
            }
            start()
        }
    }
    fun flashSuccessColor() {
        // Cambiar color a verde
        framePaint.color = colorExito
        invalidate() // Forzar redibujado

        // Regresar al azul después de 500ms
        postDelayed({
            framePaint.color = colorAzul
            invalidate()
        }, 500)
    }
    override fun onDetachedFromWindow() {
        laserAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // --- CÁLCULO DE DIMENSIONES DEL VISOR ---
        val frameWidth = width * 0.75f
        val frameHeight = height * 0.35f
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight

        rect.set(left, top, right, bottom)

        // 1. Dibujar fondo oscuro
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 2. Dibujar el agujero transparente
        canvas.drawRoundRect(rect, 50f, 50f, transparentPaint)

        // 3. Dibujar las 4 esquinas azules (Estilo corporativo)
        val cornerLineLength = 100f
        val cornerRadius = 50f
        path.reset()

        // Superior Izquierda
        path.moveTo(left, top + cornerLineLength)
        path.lineTo(left, top + cornerRadius)
        path.quadTo(left, top, left + cornerRadius, top)
        path.lineTo(left + cornerLineLength, top)

        // Superior Derecha
        path.moveTo(right - cornerLineLength, top)
        path.lineTo(right - cornerRadius, top)
        path.quadTo(right, top, right, top + cornerRadius)
        path.lineTo(right, top + cornerLineLength)

        // Inferior Izquierda
        path.moveTo(left, bottom - cornerLineLength)
        path.lineTo(left, bottom - cornerRadius)
        path.quadTo(left, bottom, left + cornerRadius, bottom)
        path.lineTo(left + cornerLineLength, bottom)

        // Inferior Derecha
        path.moveTo(right - cornerLineLength, bottom)
        path.lineTo(right - cornerRadius, bottom)
        path.quadTo(right, bottom, right, bottom - cornerRadius)
        path.lineTo(right, bottom - cornerLineLength)

        canvas.drawPath(path, framePaint)

        // 4. Dibujar el Láser animado (usando el recurso XML)
        laserDrawable?.let {
            val laserLineY = rect.top + laserYOffset

            // Definimos los límites del láser para que encaje dentro del recuadro
            val dLeft = (rect.left + 25f).toInt()
            val dRight = (rect.right - 25f).toInt()
            val dTop = (laserLineY - laserHeight / 2).toInt()
            val dBottom = (laserLineY + laserHeight / 2).toInt()

            it.setBounds(dLeft, dTop, dRight, dBottom)
            it.draw(canvas)
        }
    }

    // Método que usa BarcodeAnalyzer para mapear el área de escaneo
    fun getScanBoxRect(): RectF {
        val frameWidth = width * 0.75f
        val frameHeight = height * 0.35f
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight

        return RectF(left, top, right, bottom)
    }
}