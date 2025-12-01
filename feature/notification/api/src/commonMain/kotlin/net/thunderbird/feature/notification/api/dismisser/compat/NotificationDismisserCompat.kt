package net.thunderbird.feature.notification.api.dismisser.compat

import androidx.annotation.Discouraged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.dismisser.NotificationDismisser

/**
 * A compatibility layer for dismissing notifications from Java code.
 *
 * This class wraps [NotificationDismisser] and provides a Java-friendly API for sending notifications
 * and receiving results via a callback interface.
 *
 * It is marked as [Discouraged] because it is intended only for use within Java classes.
 * Kotlin code should use [NotificationDismisser] directly.
 *
 * @property notificationDismisser The underlying [NotificationDismisser] instance.
 * @property mainImmediateDispatcher The [CoroutineDispatcher] used for launching coroutines.
 */
@Discouraged("Only for usage within a Java class. Use NotificationDismisser instead.")
class NotificationDismisserCompat @JvmOverloads constructor(
    private val notificationDismisser: NotificationDismisser,
    mainImmediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : DisposableHandle {
    private val scope = CoroutineScope(SupervisorJob() + mainImmediateDispatcher)

    fun dismiss(notificationId: Int, onResultListener: OnResultListener) {
        notificationDismisser.dismiss(NotificationId(notificationId))
            .onEach { outcome -> onResultListener.onResult(outcome) }
            .launchIn(scope)
    }

    fun dismiss(notification: Notification, onResultListener: OnResultListener) {
        notificationDismisser.dismiss(notification)
            .onEach { outcome -> onResultListener.onResult(outcome) }
            .launchIn(scope)
    }

    override fun dispose() {
        scope.cancel()
    }

    fun interface OnResultListener {
        fun onResult(outcome: NotificationCommandOutcome<Notification>)
    }
}
