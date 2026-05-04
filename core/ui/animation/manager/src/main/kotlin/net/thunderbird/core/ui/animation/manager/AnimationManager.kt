package net.thunderbird.core.ui.animation.manager

import android.animation.ValueAnimator
import android.os.Build
import net.thunderbird.core.preference.AnimationPreference
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager

interface AnimationManager {
    fun shouldShowAnimations(): Boolean
}

class DefaultAnimationManager(
    private val visualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
) : AnimationManager {
    override fun shouldShowAnimations(): Boolean {
        return when (visualSettingsPreferenceManager.getConfig().animationPreference) {
            AnimationPreference.ON -> true
            AnimationPreference.OFF -> false
            AnimationPreference.FOLLOW_SYSTEM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ValueAnimator.areAnimatorsEnabled()
            } else {
                true
            }
        }
    }
}
