package com.kasolution.verify.core.utils.Components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.kasolution.verify.R
import com.kasolution.verify.databinding.LoadingButtonBinding

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LoadingButtonBinding =
        LoadingButtonBinding.inflate(LayoutInflater.from(context), this)

    private var originalText: String = ""
    private var isAnimating = false

    fun setText(text: String) {
        originalText = text
        binding.innerButton.text = text
    }
    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.LoadingButton)

            // 1. Leer Texto
            val text = typedArray.getString(R.styleable.LoadingButton_android_text)
            setText(text ?: "")

            // 2. Leer Color de Texto (Blanco por defecto)
            val textColor = typedArray.getColor(R.styleable.LoadingButton_android_textColor, android.graphics.Color.WHITE)
            binding.innerButton.setTextColor(textColor)

            // 3. Leer Color del Botón
            val btnColor = typedArray.getColor(R.styleable.LoadingButton_buttonColor, 0)
            if (btnColor != 0) {
                binding.innerButton.backgroundTintList = android.content.res.ColorStateList.valueOf(btnColor)
            }
            val textStyle = typedArray.getInt(R.styleable.LoadingButton_android_textStyle, 0)
            binding.innerButton.setTypeface(binding.innerButton.typeface, textStyle)
            val defaultSize = (16 * context.resources.displayMetrics.scaledDensity).toInt()
            val textSize = typedArray.getDimensionPixelSize(R.styleable.LoadingButton_android_textSize, defaultSize)

            binding.innerButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

            typedArray.recycle()
        }
    }
    fun setLoading(isLoading: Boolean) {
        if (isLoading == isAnimating) return
        isAnimating = isLoading

        if (isLoading) {
            binding.innerButton.text = ""
            binding.innerButton.isClickable = false
            binding.dotContainer.visibility = View.VISIBLE
            startAnimation()
        } else {
            binding.innerButton.text = originalText
            binding.innerButton.isClickable = true
            binding.dotContainer.visibility = View.GONE
            stopAnimation()
        }
    }

    private fun startAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { i, dot ->
            // Animación de escala para efecto de "rebote"
            dot.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(400)
                .setStartDelay(i * 150L)
                .withEndAction {
                    if (isAnimating) {
                        dot.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .withEndAction {
                                // Solo reiniciamos el ciclo al terminar el último punto
                                if (isAnimating && i == 2) startAnimation()
                            }.start()
                    }
                }.start()
        }
    }

    private fun stopAnimation() {
        listOf(binding.dot1, binding.dot2, binding.dot3).forEach { it.animate().cancel() }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        binding.innerButton.setOnClickListener(listener)
    }
}