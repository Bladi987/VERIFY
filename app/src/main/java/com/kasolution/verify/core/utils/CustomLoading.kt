package com.kasolution.verify.core.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.kasolution.verify.R
import kotlin.apply
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.ranges.coerceIn
import kotlin.ranges.until

class CustomLoading @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var dotColor: Int = Color.parseColor("#00AEEF")
        set(value) {
            field = value
            invalidate()
        }

    private var dotMaxRadius: Float = dpToPx(8f)
    private var dotCount: Int = 5

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var animationValue = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            animationValue = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        // 1. COMPORTAMIENTO AUTOMÁTICO DE ELEVACIÓN
        // Le damos una elevación por defecto para que siempre esté sobre botones
        translationZ = dpToPx(8f)

        // 2. IMPORTANTE: Evitar que el componente bloquee clics si está oculto
        // (Igual que un ProgressBar)
        isClickable = false
        isFocusable = false

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DotsLoadingView)
        try {
            dotColor = typedArray.getColor(R.styleable.DotsLoadingView_dotColor, dotColor)
            dotCount = typedArray.getInteger(R.styleable.DotsLoadingView_dotCount, 5)
            dotMaxRadius = typedArray.getDimension(R.styleable.DotsLoadingView_dotMaxRadius, dotMaxRadius)
        } finally {
            typedArray.recycle()
        }
    }

    // 3. CONTROL DE ANIMACIÓN INTELIGENTE
    // Solo gasta batería si la vista es realmente visible (como el ProgressBar real)
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            if (!animator.isRunning) animator.start()
        } else {
            animator.cancel()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode && visibility == VISIBLE) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val size = kotlin.comparisons.minOf(width, height)

        val responsiveDotRadius = kotlin.comparisons.minOf(dotMaxRadius, size * 0.12f)
        val dynamicOrbitRadius = (size / 2f) - responsiveDotRadius - (size * 0.05f)

        for (i in 0 until dotCount) {
            val stretchFactor = sin(PI * animationValue).toFloat()
            val delay = (i.toFloat() / dotCount) * 0.70f * stretchFactor
            val progress = (animationValue - delay).coerceIn(0f, animationValue)
            val angle = (2 * PI * progress) - (PI / 2)

            val x = centerX + dynamicOrbitRadius * cos(angle).toFloat()
            val y = centerY + dynamicOrbitRadius * sin(angle).toFloat()

            val sizeScale = 1f - (i.toFloat() / dotCount * 0.75f)
            paint.color = dotColor

            val alphaFactor = if (animationValue < delay * 0.5f) 0f else 1f
            paint.alpha = (((1f - (i.toFloat() / dotCount * 0.6f)) * 255) * alphaFactor).toInt()

            canvas.drawCircle(x, y, responsiveDotRadius * sizeScale, paint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}