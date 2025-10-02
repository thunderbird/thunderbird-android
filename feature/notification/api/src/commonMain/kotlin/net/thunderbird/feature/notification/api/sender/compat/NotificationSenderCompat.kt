package net.thunderbird.feature.notification.api.sender.compat

import androidx.annotation.Discouraged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.sender.NotificationSender

/**
 * A compatibility layer for sending notifications from Java code.
 *
 * This class wraps [NotificationSender] and provides a Java-friendly API for sending notifications
 * and receiving results via a callback interface.
 *
 * It is marked as [Discouraged] because it is intended only for use within Java classes.
 * Kotlin code should use [NotificationSender] directly.
 *
 * @property notificationSender The underlying [NotificationSender] instance.
 * @property mainImmediateDispatcher The [CoroutineDispatcher] used for launching coroutines.
 */
@Discouraged("Only for usage within a Java class. Use NotificationSender instead.")
class NotificationSenderCompat @JvmOverloads constructor(
    private val notificationSender: NotificationSender,
    mainImmediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : DisposableHandle {
    private val scope = CoroutineScope(SupervisorJob() + mainImmediateDispatcher)

    fun send(notification: Notification, onResultListener: OnResultListener) {
        notificationSender.send(notification)
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
