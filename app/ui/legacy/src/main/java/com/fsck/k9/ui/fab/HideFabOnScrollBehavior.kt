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
import com.google.android.material.snackbar.Snackbar.SnackbarLayout

class HideFabOnScrollBehavior(context: Context, attributes: AttributeSet) :
    HideBottomViewOnScrollBehavior<FloatingActionButton>(context, attributes) {

    override fun onAttachedToLayoutParams(lp: CoordinatorLayout.LayoutParams) {
        if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
            // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that we dodge any Snackbars
            lp.dodgeInsetEdges = Gravity.BOTTOM
        }

        super.onAttachedToLayoutParams(lp)
    }

    // FIXME restricted API
    @SuppressLint("RestrictedApi")
    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency is SnackbarLayout || super.layoutDependsOn(parent, child, dependency)
    }

    // FIXME restricted API
    @SuppressLint("RestrictedApi")
    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View,
    ): Boolean {
        if (dependency is SnackbarLayout) {
            val additionalHiddenOffsetY = dependency.height + dependency.marginBottom
            setAdditionalHiddenOffsetY(child, additionalHiddenOffsetY)
        }

        return false
    }

    // FIXME restricted API
    @SuppressLint("RestrictedApi")
    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)

        if (dependency is SnackbarLayout) {
            setAdditionalHiddenOffsetY(child, 0)
        }
    }
}
