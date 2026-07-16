package com.kasolution.verify.core.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kasolution.verify.R
import com.kasolution.verify.databinding.LayoutActionSheetUniversalBinding
import com.kasolution.verify.databinding.NewBottonSheetBinding

object NewBottonSheetHelper {

    private var currentBehavior: BottomSheetBehavior<View>? = null
    private var isClosing = false

    private fun <T : ViewBinding> prepareSheetContainer(
        activity: Activity,
        inflate: (LayoutInflater, ViewGroup, Boolean) -> T
    ): T {
        forceCleanup(activity)
        isClosing = false

        val inflater = LayoutInflater.from(activity)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val binding = inflate(inflater, rootView, false)

        // Configuración de contenedor común unificada aquí
        binding.root.tag = "SHEET_TAG"
        binding.root.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootView.addView(binding.root)
        binding.root.bringToFront()

        return binding
    }
    private fun setupBaseBehavior(
        activity: Activity,
        sheetView: View,
        scrimView: View,
        mainRootView: View,
        onDismiss: (() -> Unit)?
    ): BottomSheetBehavior<View> {
        val behavior = BottomSheetBehavior.from(sheetView)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        behavior.apply {
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN
        }

        sheetView.setOnClickListener { /* Bloqueo de clics internos */ }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN && !isClosing) {
                    cleanup(rootView, mainRootView, onDismiss)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset < 1.0f) {
                    scrimView.alpha = slideOffset.coerceIn(0f, 1f)
                }
            }
        })

        scrimView.setOnClickListener {
            hideWithAnimation(sheetView, scrimView, mainRootView, behavior, rootView, onDismiss)
        }

        return behavior
    }

    private fun showWithAnimation(sheetView: View, scrimView: View, behavior: BottomSheetBehavior<View>) {
        sheetView.visibility = View.VISIBLE
        scrimView.alpha = 0f
        scrimView.animate().alpha(1f).setDuration(300).start()

        val anim = AnimationUtils.loadAnimation(sheetView.context, R.anim.ios_sheet_entrance)
        sheetView.startAnimation(anim)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hideWithAnimation(
        sheetView: View,
        scrimView: View,
        mainRootView: View,
        behavior: BottomSheetBehavior<View>,
        rootView: ViewGroup,
        onDismiss: (() -> Unit)?
    ) {
        if (isClosing) return
        isClosing = true

        val anim = AnimationUtils.loadAnimation(sheetView.context, R.anim.ios_sheet_exit)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                cleanup(rootView, mainRootView, onDismiss)
            }
            override fun onAnimationStart(p0: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(p0: android.view.animation.Animation?) {}
        })

        sheetView.startAnimation(anim)
        scrimView.animate().alpha(0f).setDuration(250).start()
    }

    private fun cleanup(rootView: ViewGroup, mainRootView: View, onDismiss: (() -> Unit)?) {
        rootView.post {
            if (mainRootView.parent != null) {
                rootView.removeView(mainRootView)
            }
            clearState()
            onDismiss?.invoke()
        }
    }

    fun showInventoryOptions(
        activity: Activity,
        cabeceraName: String,
        name: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        // Usamos la función genérica pasándole el inflador correspondiente
        val binding = prepareSheetContainer(activity, LayoutActionSheetUniversalBinding::inflate)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        binding.tvHeader.text = cabeceraName
        binding.tvSelectedName.text = name

        val behavior = setupBaseBehavior(activity, binding.layoutOptions, binding.viewScrim, binding.root, onDismiss)
        currentBehavior = behavior

        binding.btnEditOption.setOnClickListener {
            onEdit()
            hideWithAnimation(binding.layoutOptions, binding.viewScrim, binding.root, behavior, rootView, onDismiss)
        }

        binding.btnDeleteOption.setOnClickListener {
            onDelete()
            hideWithAnimation(binding.layoutOptions, binding.viewScrim, binding.root, behavior, rootView, onDismiss)
        }

        showWithAnimation(binding.layoutOptions, binding.viewScrim, behavior)
    }

    fun showEmployeeOptions(
        activity: Activity,
        cabeceraName: String,
        name: String,
        onEdit: () -> Unit,
        onPermissions: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        // Al usar la función genérica, reducimos drásticamente las líneas repetidas
        val binding = prepareSheetContainer(activity, NewBottonSheetBinding::inflate)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        binding.tvHeader.text = cabeceraName
        binding.tvSelectedName.text = name

        val behavior = setupBaseBehavior(activity, binding.layoutOptions, binding.viewScrim, binding.root, onDismiss)
        currentBehavior = behavior

        binding.btnEditOption.setOnClickListener {
            onEdit()
            hideWithAnimation(binding.layoutOptions, binding.viewScrim, binding.root, behavior, rootView, onDismiss)
        }

        binding.btnPermissionsOption.setOnClickListener {
            onPermissions()
            hideWithAnimation(binding.layoutOptions, binding.viewScrim, binding.root, behavior, rootView, onDismiss)
        }

        binding.btnDeleteOption.setOnClickListener {
            onDelete()
            hideWithAnimation(binding.layoutOptions, binding.viewScrim, binding.root, behavior, rootView, onDismiss)
        }

        showWithAnimation(binding.layoutOptions, binding.viewScrim, behavior)
    }

    fun isSheetVisible(): Boolean = currentBehavior != null && currentBehavior?.state != BottomSheetBehavior.STATE_HIDDEN

    fun closeSheetDirectly() { currentBehavior?.state = BottomSheetBehavior.STATE_HIDDEN }

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