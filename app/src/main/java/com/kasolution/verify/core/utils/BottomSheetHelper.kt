package com.kasolution.verify.core.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kasolution.verify.R
import com.kasolution.verify.databinding.LayoutActionSheetUniversalBinding

object BottomSheetHelper {

    private var currentBehavior: BottomSheetBehavior<View>? = null
    private var isClosing = false

    /**
     * FUNCIÓN BASE: Configura el comportamiento, el Scrim y la limpieza.
     */
    private fun createBaseBottomSheet(
        rootView: ViewGroup,
        binding: LayoutActionSheetUniversalBinding,
        onDismiss: (() -> Unit)? = null
    ): BottomSheetBehavior<View> {

        val sheetView = binding.layoutOptions
        val scrimView = binding.viewScrim
        val behavior = BottomSheetBehavior.from(sheetView as View)

        behavior.apply {
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Bloqueo de clics internos
        sheetView.setOnClickListener { /* No hace nada */ }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN && !isClosing) {
                    cleanup(rootView, binding, onDismiss)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset < 1.0f) {
                    binding.viewScrim.alpha = slideOffset.coerceIn(0f, 1f)
                }
            }
        })

        scrimView.setOnClickListener {
            hideWithAnimation(binding, behavior, rootView, onDismiss)
        }

        return behavior
    }

    fun showInventoryOptions(
        activity: Activity,
        cabeceraName: String,
        name: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        // SEGURIDAD: Limpieza preventiva si quedó algo colgado
        forceCleanup(activity)

        isClosing = false
        val inflater = LayoutInflater.from(activity)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val binding = LayoutActionSheetUniversalBinding.inflate(inflater, rootView, false)

        binding.root.tag = "SHEET_TAG"
        binding.root.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        rootView.addView(binding.root)
        binding.root.bringToFront()
        binding.tvHeader.text = cabeceraName
        binding.tvSelectedName.text = name

        val behavior = createBaseBottomSheet(rootView, binding, onDismiss)
        currentBehavior = behavior

        binding.btnEditOption.setOnClickListener {
            onEdit()
            hideWithAnimation(binding, behavior, rootView, onDismiss)
        }

        binding.btnDeleteOption.setOnClickListener {
            onDelete()
            hideWithAnimation(binding, behavior, rootView, onDismiss)
        }

        showWithAnimation(binding, behavior)
    }

    private fun showWithAnimation(
        binding: LayoutActionSheetUniversalBinding,
        behavior: BottomSheetBehavior<View>
    ) {
        binding.layoutOptions.visibility = View.VISIBLE

        // OPCIÓN A: Si en el XML está en 1, pero quieres que aparezca suave:
        binding.viewScrim.alpha = 0f
        binding.viewScrim.animate().alpha(1f).setDuration(300).start()

        val anim = AnimationUtils.loadAnimation(binding.root.context, R.anim.ios_sheet_entrance)
        binding.layoutOptions.startAnimation(anim)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideWithAnimation(
        binding: LayoutActionSheetUniversalBinding,
        behavior: BottomSheetBehavior<View>,
        rootView: ViewGroup,
        onDismiss: (() -> Unit)?
    ) {
        if (isClosing) return
        isClosing = true

        val anim = AnimationUtils.loadAnimation(binding.root.context, R.anim.ios_sheet_exit)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                cleanup(rootView, binding, onDismiss)
            }

            override fun onAnimationStart(p0: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(p0: android.view.animation.Animation?) {}
        })

        binding.layoutOptions.startAnimation(anim)
        binding.viewScrim.animate().alpha(0f).setDuration(250).start()
    }

    private fun cleanup(
        rootView: ViewGroup,
        binding: LayoutActionSheetUniversalBinding,
        onDismiss: (() -> Unit)?
    ) {
        rootView.post {
            if (binding.root.parent != null) {
                rootView.removeView(binding.root)
            }
            clearState()
            onDismiss?.invoke()
        }
    }

    // --- MÉTODOS DE CONTROL PARA LA ACTIVITY ---

    fun isSheetVisible(): Boolean {
        return currentBehavior != null && currentBehavior?.state != BottomSheetBehavior.STATE_HIDDEN
    }

    fun closeSheetDirectly() {
        currentBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun clearState() {
        currentBehavior = null
        isClosing = false
    }
    fun forceCleanup(activity: Activity? = null) {
        activity?.let {
            val rootView = it.findViewById<ViewGroup>(android.R.id.content)
            val view = rootView.findViewWithTag<View>("SHEET_TAG")
            if (view != null) rootView.removeView(view)
        }
        clearState()
    }
}