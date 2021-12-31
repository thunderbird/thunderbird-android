package com.fsck.k9.view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.widget.ViewAnimator
import com.fsck.k9.preferences.GeneralSettingsManager

/**
 * A {@link ViewAnimator} that animates between two child views using different animations
 * depending on which view is displayed.
 */
class ViewSwitcher(context: Context, attrs: AttributeSet) : ViewAnimator(context, attrs), Animation.AnimationListener {
    lateinit var firstInAnimation: Animation
    lateinit var firstOutAnimation: Animation
    lateinit var secondInAnimation: Animation
    lateinit var secondOutAnimation: Animation
    lateinit var onSwitchCompleteListener: OnSwitchCompleteListener
    lateinit var generalSettingsManager: GeneralSettingsManager

    fun showFirstView() {
        if (displayedChild == 0) {
            return
        }
        setupAnimations(firstInAnimation, firstOutAnimation)
        displayedChild = 0
        handleSwitchCompleteCallback()
    }

    fun showSecondView() {
        if (displayedChild == 1) {
            return
        }
        setupAnimations(secondInAnimation, secondOutAnimation)
        displayedChild = 1
        handleSwitchCompleteCallback()
    }

    private fun setupAnimations(`in`: Animation?, out: Animation?) {
        if (generalSettingsManager.getSettings().showAnimations) {
            inAnimation = `in`
            outAnimation = out
            out!!.setAnimationListener(this)
        } else {
            inAnimation = null
            outAnimation = null
        }
    }

    private fun handleSwitchCompleteCallback() {
        if (!generalSettingsManager.getSettings().showAnimations) {
            onAnimationEnd(null)
        }
    }

    override fun onAnimationEnd(animation: Animation?) {
        onSwitchCompleteListener.onSwitchComplete(displayedChild)
    }

    override fun onAnimationRepeat(animation: Animation?) {
        // unused
    }

    override fun onAnimationStart(animation: Animation?) {
        // unused
    }

    interface OnSwitchCompleteListener {
        /**
         * This method will be called after the switch (including animation) has ended.
         *
         * @param displayedChild
         * Contains the zero-based index of the child view that is now displayed.
         */
        fun onSwitchComplete(displayedChild: Int)
    }
}
