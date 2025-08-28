package net.thunderbird.feature.notification.api.receiver.compat

import androidx.annotation.Discouraged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver

/**
 * A compatibility class for [InAppNotificationReceiver] that allows Java classes to observe notification events.
 *
 * This class is discouraged for use in Kotlin code. Use [InAppNotificationReceiver] directly instead.
 *
 * @param notificationReceiver The [InAppNotificationReceiver] instance to delegate to.
 * @param listener A callback function that will be invoked when a new [InAppNotificationEvent] is received.
 * @param mainImmediateDispatcher The [CoroutineDispatcher] to use for observing events.
 */
@Discouraged("Only for usage within a Java class. Use InAppNotificationReceiver instead.")
class InAppNotificationReceiverCompat(
    private val notificationReceiver: InAppNotificationReceiver,
    listener: OnReceiveEventListener,
    mainImmediateDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : InAppNotificationReceiver by notificationReceiver, DisposableHandle {
    private val scope = CoroutineScope(SupervisorJob() + mainImmediateDispatcher)

    init {
        scope.launch {
            events.collect { event ->
                listener.onReceiveEvent(event)
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }

    fun interface OnReceiveEventListener {
        fun onReceiveEvent(event: InAppNotificationEvent)
    }
}
