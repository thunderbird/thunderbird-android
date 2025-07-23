package net.thunderbird.feature.notification.api.content

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.feature.notification.api.LockscreenNotificationAppearance
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationGroup
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

/**
 * Represents a notification that can be displayed to the user.
 *
 * This interface defines the common properties that all notifications must have.
 * Must not be directly implemented. You must extend [AppNotification] instead.
 *
 * @property id The unique identifier of the notification.
 * @property title The title of the notification.
 * @property accessibilityText The text to be used for accessibility purposes.
 * @property contentText The main content text of the notification, can be null.
 * @property severity The severity level of the notification.
 * @property createdAt The date and time when the notification was created.
 * @property actions A set of actions that can be performed on the notification.
 * @property icon The notification icon.
 * @see AppNotification
 */
sealed interface Notification {
    val id: NotificationId
    val title: String
    val accessibilityText: String
    val contentText: String?
    val severity: NotificationSeverity
    val createdAt: LocalDateTime
    val actions: Set<NotificationAction>
    val icon: NotificationIcon
}

/**
 * The abstract implementation of [Notification], representing an app notification.
 * This abstraction is meant to provide default properties implementation to easy the app notification creation.
 *
 * @property accessibilityText The text that will be read by accessibility services.
 * Defaults to the notification's title.
 * @property createdAt The timestamp when the notification was created. Defaults to the current UTC time.
 * @property actions A set of actions that can be performed on the notification. Defaults to an empty set.
 * @see Notification
 */
sealed class AppNotification : Notification {
    override val accessibilityText: String = title

    @OptIn(ExperimentalTime::class)
    override val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(timeZone = TimeZone.UTC)
    override val actions: Set<NotificationAction> = emptySet()
}

/**
 * Represents a notification displayed by the system, **requiring user permission**.
 * This type of notification can appear on the lock screen.
 *
 * @property subText Additional text displayed below the content text, can be null.
 * @property channel The notification channel to which this notification belongs.
 * @property group The notification group to which this notification belongs, can be null.
 * @see LockscreenNotificationAppearance
 */
sealed interface SystemNotification : Notification {
    val subText: String? get() = null
    val channel: NotificationChannel
    val group: NotificationGroup? get() = null

    /**
     * Converts this notification to a [LockscreenNotification].
     *
     * This function should be overridden by subclasses that can be displayed on the lockscreen.
     * If the notification should not be displayed on the lockscreen, this function should return `null`.
     *
     * @return The [LockscreenNotification] representation of this notification, or `null` if it should not be
     * displayed on the lockscreen.
     */
    fun asLockscreenNotification(): LockscreenNotification? = null

    /**
     * Represents a notification that can be displayed on the lock screen.
     *
     * @property notification The system notification to be displayed.
     * @property lockscreenNotificationAppearance The appearance of the notification on the lock screen.
     * Defaults to [LockscreenNotificationAppearance.Public].
     */
    data class LockscreenNotification(
        val notification: SystemNotification,
        val lockscreenNotificationAppearance: LockscreenNotificationAppearance =
            LockscreenNotificationAppearance.Public,
    )
}

/**
 *
 * Represents a notification displayed within the application.
 *
 * In-app notifications are typically less intrusive than system notifications and **do not require**
 * system notification permissions to be displayed.
 */
sealed interface InAppNotification : Notification
