package net.thunderbird.feature.notification.api.ui.style.builder

import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle.BannerGlobalNotification
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle.BannerInlineNotification
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle.DialogNotification
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle.SnackbarNotification
import net.thunderbird.feature.notification.api.ui.style.NotificationStyleMarker
import net.thunderbird.feature.notification.api.ui.style.SnackbarDuration

/**
 * Builder for creating [InAppNotificationStyle] instances.
 * This interface defines the methods available for configuring the style of an in-app notification.
 */
class InAppNotificationStyleBuilder internal constructor() {
    private var style: InAppNotificationStyle = InAppNotificationStyle.Undefined

    /**
     * Use inline error banners to surface issues that must be resolved before the user can continue
     * with the main task or content on the screen.
     *
     * ### USAGE GUIDELINES
     *
     * #### Use for:
     * - Critical errors that disrupts a function of the screen’s functionality
     * - Errors that require user attention but do not completely block their ability to continue
     * interacting with the app
     *
     * #### Do not use for:
     * - Blocking errors that must halt the user’s flow until resolved (consider using a
     * [DialogNotification] instead)
     * - Global or persistent application states that should be shown across all screens
     * (consider using a [BannerGlobalNotification])
     * - Secondary or surface-level errors caused by a deeper issue (e.g., inability to encrypt is a warning,
     * while the missing encryption key is the actual error)
     * - Non-error messages such as warnings, success confirmations, or informational notices (these will use a
     * different component and are not part of the in-app error banner pattern)
     */
    @NotificationStyleMarker
    fun bannerInline() {
        checkSingleStyleEntry<BannerInlineNotification>()
        style = BannerInlineNotification
    }

    /**
     * Used to maintain user awareness of a persistent, irregular state of the application without
     * interrupting the primary flow. This component is appropriate for warnings that apply globally
     * across the app.
     *
     * If the warning is caused by a critical error, an [BannerInlineNotification] should also be shown
     * in the relevant context (e.g., the message list) to guide direct resolution.
     *
     * ### USAGE GUIDELINES
     *
     * #### Use for:
     * - Persistent application states that affect the current screen
     * - In account configuration flows, to display:
     * - Errors, success, or informational messages that require a constant on-screen indicator
     * - Outside of account configuration, for global warnings such as:
     * - Being offline
     * - Encryption being unavailable
     *
     * #### Do not use for:
     * - Errors, success, or informational messages outside the account configuration flow (use
     * [BannerInlineNotification] or other transient messaging components instead)
     * - Warnings that must interrupt the user’s flow or require immediate action (consider using
     * a [DialogNotification] in these cases)
     */
    @NotificationStyleMarker
    fun bannerGlobal(priority: Int = 0) {
        checkSingleStyleEntry<BannerGlobalNotification>()
        style = BannerGlobalNotification(priority = priority)
    }

    /**
     * Snackbars are used to inform the user of an error or process outcome, and may optionally offer
     * a related action. They appear temporarily without interrupting the user's current task.
     *
     * ### USAGE GUIDELINES
     *
     * #### Use for:
     * - Providing feedback when an action fails, with the option for the user to take corrective action
     *
     * #### Do not use for:
     * - Errors that must interrupt the user’s flow or block further interaction (use a [DialogNotification]
     * in these cases)
     * - Account sync error feedback in the Unified Inbox (use a [BannerInlineNotification] or
     * [BannerGlobalNotification] for that context)
     */
    @NotificationStyleMarker
    fun snackbar(duration: SnackbarDuration = SnackbarDuration.Short) {
        checkSingleStyleEntry<SnackbarNotification>()
        style = SnackbarNotification(duration)
    }

    /**
     * Used to inform the user about a required permission needed to enable or complete a key feature of the app.
     * The dialog provides a concise explanation of the need for the permission and prompts the user to grant it.
     *
     * ### USAGE GUIDELINES
     *
     * #### Use for:
     * - Requesting notification permission from the user
     * - Clearly and succinctly explaining why the permission is needed and how it impacts the app experience
     *
     * #### Do not use for:
     * - Displaying errors
     * - Requesting contacts permission, as missing access does not critically affect app functionality
     * - Requesting background activity permission related to battery saver, since the app cannot reliably detect
     * the current permission state
     */
    @NotificationStyleMarker
    fun dialog() {
        checkSingleStyleEntry<DialogNotification>()
        style = DialogNotification
    }

    /**
     * Builds the [InAppNotificationStyle] based on the provided parameters.
     *
     * @return The constructed [InAppNotificationStyle].
     */
    fun build(): InAppNotificationStyle {
        check(style != InAppNotificationStyle.Undefined) {
            "You must pick one in-app notification style."
        }
        return style
    }

    private inline fun <reified T> checkSingleStyleEntry() {
        check(style == InAppNotificationStyle.Undefined) {
            "An in-app notification can only have one type of style. " +
                "Current style is ${style::class.simpleName}, trying to set ${T::class.simpleName}."
        }
    }
}
