package net.thunderbird.feature.notification.api.receiver.compat

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
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver
import net.thunderbird.feature.notification.api.receiver.compat.InAppNotificationReceiverCompat.OnReceiveEventListener
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification

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
                    InAppNotificationEvent.Show(FakeInAppOnlyNotification(title = title))
                }

                else -> InAppNotificationEvent.Dismiss(FakeInAppOnlyNotification(title = title))
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
}
