/*
 * Copyright (C) 2023 K-9 Mail contributors
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.k9mail.ui.utils.bottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable

/**
 * Modal bottom sheet that displays a toolbar when it is fully expanded. Only meant to be used with
 * [ToolbarBottomSheetDialogFragment].
 *
 * Based on [com.google.android.material.bottomsheet.BottomSheetDialog].
 */
class ToolbarBottomSheetDialog internal constructor(context: Context, @StyleRes theme: Int) :
    AppCompatDialog(context, getThemeResId(context, theme)) {

    private var internalBehavior: LayoutAwareBottomSheetBehavior<FrameLayout>? = null
    val behavior: LayoutAwareBottomSheetBehavior<*>
        get() {
            if (internalBehavior == null) {
                ensureContainerAndBehavior()
            }

            return internalBehavior ?: error("Missing ToolbarBottomSheetBehavior")
        }

    private var container: FrameLayout? = null
    private var coordinator: CoordinatorLayout? = null
    private var bottomSheet: FrameLayout? = null

    var toolbar: Toolbar? = null
        private set

    /**
     * `true` if dismissing will perform the swipe down animation on the bottom sheet, rather than the window animation
     * for the dialog.
     */
    var isDismissWithAnimation = false

    private var toolbarVisibilityCallback: LayoutAwareBottomSheetCallback? = null

    init {
        // We hide the title bar for any style configuration. Otherwise, there will be a gap above the bottom sheet
        // when it is expanded.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCancelable(true)

        val window = window
        if (window != null) {
            // The status bar should always be transparent because of the window animation.
            window.statusBarColor = Color.TRANSPARENT
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            if (VERSION.SDK_INT < VERSION_CODES.M) {
                // It can be transparent for API 23 and above because we will handle switching the status
                // bar icons to light or dark as appropriate. For API 21 and API 22 we just set the
                // translucent status bar.
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }

            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        error("Not supported")
    }

    override fun setContentView(view: View) {
        super.setContentView(wrapInBottomSheet(view))
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        error("Not supported")
    }

    override fun onStart() {
        super.onStart()

        internalBehavior?.let { behavior ->
            if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val window = window
        if (window != null) {
            container?.fitsSystemWindows = false
            coordinator?.fitsSystemWindows = false

            val flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or flags
        }
    }

    /**
     * This function can be called from a few different use cases, including Swiping the dialog down or calling
     * `dismiss()` from a `BottomSheetDialogFragment`, tapping outside a dialog, etc…
     *
     * The default animation to dismiss this dialog is a fade-out transition through a
     * windowAnimation. Call `setDismissWithAnimation(true)` if you want to utilize the BottomSheet animation
     * instead.
     *
     * If this function is called from a swipe down interaction, or dismissWithAnimation is false,
     * then keep the default behavior.
     *
     * Else, since this is a terminal event which will finish this dialog, we override the attached
     * [BottomSheetCallback] to call this function, after [BottomSheetBehavior.STATE_HIDDEN] is set. This
     * will enforce the swipe down animation before canceling this dialog.
     */
    override fun cancel() {
        val behavior = this.behavior

        if (!isDismissWithAnimation || behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            super.cancel()
        } else {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    private fun ensureContainerAndBehavior(): FrameLayout? {
        if (container == null) {
            val container = View.inflate(context, R.layout.design_bottom_sheet_dialog, null) as FrameLayout
            this.container = container

            coordinator = container.findViewById(R.id.coordinator) ?: error("View not found")
            val bottomSheet = container.findViewById<FrameLayout>(R.id.design_bottom_sheet) ?: error("View not found")
            this.bottomSheet = bottomSheet

            toolbar = bottomSheet.findViewById(R.id.toolbar)

            val behavior = LayoutAwareBottomSheetBehavior.from(bottomSheet).apply {
                addBottomSheetCallback(cancelDialogCallback)
                isHideable = true
            }
            this.internalBehavior = behavior

            container.doOnLayout {
                behavior.peekHeight = container.height / 2
            }

            bottomSheet.doOnLayout {
                // Don't draw the toolbar underneath the status bar if the bottom sheet doesn't cover the whole screen
                // anyway.
                if (bottomSheet.width < container.width) {
                    container.fitsSystemWindows = true
                    coordinator?.fitsSystemWindows = true
                    setToolbarVisibilityCallback(topInset = 0)
                } else {
                    container.fitsSystemWindows = false
                    coordinator?.fitsSystemWindows = false
                }
            }
        }

        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun wrapInBottomSheet(view: View): View {
        ensureContainerAndBehavior()

        val container = checkNotNull(container)
        val coordinator = checkNotNull(coordinator)
        val bottomSheet = checkNotNull(bottomSheet)

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topInset = insets.top

            setToolbarVisibilityCallback(topInset)

            WindowInsetsCompat.CONSUMED
        }

        while (bottomSheet.childCount > NUMBER_OF_OWN_BOTTOM_SHEET_CHILDREN) {
            bottomSheet.removeViewAt(0)
        }

        bottomSheet.addView(view, 0)

        // We treat the CoordinatorLayout as outside the dialog though it is technically inside
        coordinator.findViewById<View>(R.id.touch_outside).setOnClickListener {
            if (isShowing) {
                cancel()
            }
        }

        // Handle accessibility events
        ViewCompat.setAccessibilityDelegate(
            bottomSheet,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)

                    info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS)
                    info.isDismissable = true
                }

                override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                    if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS) {
                        cancel()
                        return true
                    }

                    return super.performAccessibilityAction(host, action, args)
                }
            },
        )

        bottomSheet.setOnTouchListener { _, _ ->
            // Consume the event and prevent it from falling through
            true
        }

        return container
    }

    private fun setToolbarVisibilityCallback(topInset: Int) {
        val window = checkNotNull(window)
        val bottomSheet = checkNotNull(bottomSheet)
        val toolbar = checkNotNull(toolbar)
        val behavior = checkNotNull(internalBehavior)

        toolbarVisibilityCallback?.let { oldCallback ->
            behavior.removeBottomSheetCallback(oldCallback)
        }

        val windowInsetsController = WindowCompat.getInsetsController(window, bottomSheet)
        val newCallback = ToolbarVisibilityCallback(windowInsetsController, toolbar, topInset, behavior)
        behavior.addBottomSheetCallback(newCallback)

        this.toolbarVisibilityCallback = newCallback
    }

    fun removeDefaultCallback() {
        checkNotNull(internalBehavior).removeBottomSheetCallback(cancelDialogCallback)
    }

    private val cancelDialogCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                cancel()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    private class ToolbarVisibilityCallback(
        private val windowInsetsController: WindowInsetsControllerCompat,
        private val toolbar: Toolbar,
        private val topInset: Int,
        private val behavior: BottomSheetBehavior<FrameLayout>,
    ) : LayoutAwareBottomSheetCallback() {

        private val lightStatusBar: Boolean = windowInsetsController.isAppearanceLightStatusBars
        private var lightToolbar = false

        init {
            // Try to find the background color to automatically change the status bar icons so they will
            // still be visible when the bottomsheet slides underneath the status bar.
            val toolbarBackground = toolbar.background
            val backgroundTint = if (toolbarBackground is MaterialShapeDrawable) {
                toolbarBackground.fillColor
            } else {
                ViewCompat.getBackgroundTintList(toolbar)
            }

            lightToolbar = if (backgroundTint != null) {
                // First check for a tint
                MaterialColors.isColorLight(backgroundTint.defaultColor)
            } else if (toolbar.background is ColorDrawable) {
                // Then check for the background color
                MaterialColors.isColorLight((toolbar.background as ColorDrawable).color)
            } else {
                // Otherwise don't change the status bar color
                lightStatusBar
            }
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            handleToolbarVisibility(bottomSheet)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            handleToolbarVisibility(bottomSheet)
        }

        override fun onBottomSheetLayout(bottomSheet: View) {
            handleToolbarVisibility(bottomSheet)
        }

        private fun handleToolbarVisibility(bottomSheet: View) {
            setToolbarState(bottomSheet)
            setStatusBarColor(bottomSheet)
        }

        private fun setToolbarState(bottomSheet: View) {
            val top = bottomSheet.top

            val collapsedOffset = (bottomSheet.parent as View).height / 2
            if (top >= collapsedOffset || behavior.expandedOffset > 0) {
                toolbar.isInvisible = true
                bottomSheet.setPadding(0, 0, 0, 0)
                return
            }

            val toolbarHeight = toolbar.height - toolbar.paddingTop
            val toolbarHeightAndInset = toolbarHeight + topInset
            val expandedPercentage =
                ((collapsedOffset - top).toFloat() / (collapsedOffset - behavior.expandedOffset)).coerceAtMost(1f)

            // Add top padding to bottom sheet to make room for the toolbar
            val paddingTop = (toolbarHeightAndInset * expandedPercentage).toInt().coerceAtLeast(0)
            bottomSheet.setPadding(0, paddingTop, 0, 0)

            // Start showing the toolbar when the bottom sheet is a toolbar height away from the top of the screen.
            // This value was chosen rather arbitrarily because it looked nice enough.
            val toolbarPercentage =
                ((toolbarHeight - top).toFloat() / (toolbarHeight - behavior.expandedOffset)).coerceAtLeast(0f)

            if (toolbarPercentage > 0) {
                toolbar.isVisible = true

                // Set the toolbar's top padding so the toolbar covers the bottom sheet's whole top padding
                val toolbarPaddingTop = (paddingTop - toolbarHeight).coerceAtLeast(0)
                toolbar.setPadding(0, toolbarPaddingTop, 0, 0)

                // Translate the toolbar view so it is drawn on top of the bottom sheet's top padding
                toolbar.translationY = -paddingTop.toFloat()

                toolbar.alpha = toolbarPercentage
            } else {
                toolbar.isInvisible = true
            }
        }

        private fun setStatusBarColor(bottomSheet: View) {
            val toolbarLightThreshold = topInset / 2
            if (bottomSheet.top < toolbarLightThreshold) {
                windowInsetsController.isAppearanceLightStatusBars = lightToolbar
            } else if (bottomSheet.top != 0) {
                windowInsetsController.isAppearanceLightStatusBars = lightStatusBar
            }
        }
    }

    companion object {
        private const val NUMBER_OF_OWN_BOTTOM_SHEET_CHILDREN = 1

        private fun getThemeResId(context: Context, themeId: Int): Int {
            if (themeId != 0) return themeId

            val outValue = TypedValue()

            val wasAttributeResolved = context.theme.resolveAttribute(
                com.google.android.material.R.attr.bottomSheetDialogTheme,
                outValue,
                true,
            )

            return if (wasAttributeResolved) {
                outValue.resourceId
            } else {
                error("Missing bottomSheetDialogTheme attribute in theme")
            }
        }
    }
}
