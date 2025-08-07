package net.thunderbird.feature.notification.api.ui.host.visual

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet

/**
 * Defines the visual representation of in-app notifications.
 *
 * This interface holds the visual data for different types of in-app notifications
 * that can be displayed to the user. It allows for a structured way to manage
 * and present notification information.
 */
@Stable
internal interface InAppNotificationHostState {
    /**
     * The visual representation of a global banner notification.
     *
     * This property holds a [BannerGlobalVisual] object if a global banner is
     * currently active, or `null` if no global banner is being shown.
     */
    val bannerGlobalVisual: BannerGlobalVisual?

    /**
     * A set of inline banner visuals that are currently active.
     */
    val bannerInlineVisuals: ImmutableSet<BannerInlineVisual>

    /**
     * The visual representation of a snackbar notification.
     *
     * This property holds a [SnackbarVisual] object if a snackbar notification
     * is currently active, or `null` if no snackbar is being displayed.
     */
    val snackbarVisual: SnackbarVisual?

    /**
     * Dismisses the given in-app notification visual.
     *
     * This function is responsible for removing the specified notification visual
     * from the display.
     *
     * @param visual The [InAppNotificationVisual] to dismiss.
     */
    fun dismiss(visual: InAppNotificationVisual)
}
