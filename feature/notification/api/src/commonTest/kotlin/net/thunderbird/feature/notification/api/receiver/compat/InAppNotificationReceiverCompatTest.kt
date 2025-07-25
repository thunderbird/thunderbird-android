package net.thunderbird.feature.notification.api.receiver.compat

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.AppNotification
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver
import net.thunderbird.feature.notification.api.receiver.compat.InAppNotificationReceiverCompat.OnReceiveEventListener
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

class InAppNotificationReceiverCompatTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onReceiveEvent should be triggered when an event is received into InAppNotificationReceiver`() = runTest {
        // Arrange
        val inAppNotificationReceiver = FakeInAppNotificationReceiver()
        val eventsTriggered = mutableListOf<InAppNotificationEvent>()
        val expectedEvents = List(size = 100) { index ->
            val title = "notification $index"
            when {
                index % 2 == 0 -> {
                    InAppNotificationEvent.Show(FakeNotification(title = title))
                }

                else -> InAppNotificationEvent.Dismiss(FakeNotification(title = title))
            }
        }.toTypedArray()
        val onReceiveEventListener = spy<OnReceiveEventListener>(
            OnReceiveEventListener { event ->
                eventsTriggered += event
            },
        )
        InAppNotificationReceiverCompat(
            notificationReceiver = inAppNotificationReceiver,
            listener = onReceiveEventListener,
            mainImmediateDispatcher = UnconfinedTestDispatcher(),
        )

        // Act
        expectedEvents.forEach { event -> inAppNotificationReceiver.trigger(event) }

        // Assert
        verify(exactly(100)) {
            onReceiveEventListener.onReceiveEvent(event = any())
        }
        assertThat(eventsTriggered).containsExactlyInAnyOrder(elements = expectedEvents)
    }

    private class FakeInAppNotificationReceiver : InAppNotificationReceiver {
        private val _events = MutableSharedFlow<InAppNotificationEvent>()
        override val events: SharedFlow<InAppNotificationEvent> = _events.asSharedFlow()

        suspend fun trigger(event: InAppNotificationEvent) {
            _events.emit(event)
        }
    }

    data class FakeNotification(
        override val title: String = "fake title",
        override val contentText: String? = "fake content",
        override val severity: NotificationSeverity = NotificationSeverity.Information,
        override val icon: NotificationIcon = NotificationIcon(
            inAppNotificationIcon = ImageVector.Builder(
                defaultWidth = 0.dp,
                defaultHeight = 0.dp,
                viewportWidth = 0f,
                viewportHeight = 0f,
            ).build(),
        ),
    ) : AppNotification(), InAppNotification
}
