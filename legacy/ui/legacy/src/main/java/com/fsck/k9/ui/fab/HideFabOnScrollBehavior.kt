package com.fsck.k9.ui.fab

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout

/**
 * Hides the floating action button when the `CoordinatorLayout` is scrolled.
 *
 * The behavior to hide the FAB when the [CoordinatorLayout] is scrolled is provided by the super class
 * [HideBottomViewOnScrollBehavior]. The code in this class adjusts the vertical position of the FAB when a [Snackbar]
 * is visible. This is necessary because we (like many others) deliberately ignore the
 * [guideline](https://m3.material.io/components/snackbar/guidelines) to display a `Snackbar` above the FAB.
 */
class HideFabOnScrollBehavior(context: Context, attributes: AttributeSet) :
    HideBottomViewOnScrollBehavior<FloatingActionButton>(context, attributes) {

    override fun onAttachedToLayoutParams(lp: CoordinatorLayout.LayoutParams) {
        if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
            // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that we dodge any Snackbars
            lp.dodgeInsetEdges = Gravity.BOTTOM
        }

        super.onAttachedToLayoutParams(lp)
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency.isSnackbarLayout() || super.layoutDependsOn(parent, child, dependency)
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View,
    ): Boolean {
        if (dependency.isSnackbarLayout()) {
            val additionalHiddenOffsetY = dependency.height + dependency.marginBottom
            setAdditionalHiddenOffsetY(child, additionalHiddenOffsetY)
        }

        return false
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)

        if (dependency.isSnackbarLayout()) {
            setAdditionalHiddenOffsetY(child, 0)
        }
    }

    // SnackbarLayout is a restricted type that shouldn't be accessed from outside of its library. However, there
    // doesn't seem to be a public API we could use to implement the desired behavior.
    @SuppressLint("RestrictedApi")
    private fun View.isSnackbarLayout(): Boolean {
        return this is SnackbarLayout
    }
}
