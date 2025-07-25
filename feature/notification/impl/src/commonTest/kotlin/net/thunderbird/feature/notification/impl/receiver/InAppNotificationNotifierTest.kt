package net.thunderbird.feature.notification.impl.receiver

import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification

class InAppNotificationNotifierTest {
    @Test
    fun `show should not publish event when notification is already present in NotificationRegistry`() = runTest {
        // Arrange
        val notificationId = NotificationId(value = 1)
        val notification = FakeInAppOnlyNotification()
        val registrar = mapOf(notificationId to notification)
        val eventBus = spy(InAppNotificationEventBus())
        val testSubject = createTestSubject(registrar, eventBus)

        // Act
        testSubject.show(notificationId, notification)

        // Assert
        verifySuspend(exactly(1)) {
            eventBus.publish(any())
        }
    }

    @Test
    fun `show should publish event when notification is not present in NotificationRegistry`() = runTest {
        // Arrange
        val notificationId = NotificationId(value = Int.MAX_VALUE)
        val notification = FakeInAppOnlyNotification()
        val registrar = buildMap<NotificationId, Notification> {
            repeat(times = 100) { index ->
                put(NotificationId(index), FakeInAppOnlyNotification(title = "fake title $index"))
            }
        }
        val eventBus = spy(InAppNotificationEventBus())
        val testSubject = createTestSubject(registrar, eventBus)

        // Act
        testSubject.show(notificationId, notification)

        // Assert
        verifySuspend(exactly(0)) {
            eventBus.publish(any())
        }
    }

    private fun createTestSubject(
        registrar: Map<NotificationId, Notification>,
        eventBus: InAppNotificationEventBus,
    ): InAppNotificationNotifier {
        return InAppNotificationNotifier(
            logger = TestLogger(),
            notificationRegistry = object : NotificationRegistry {
                override val registrar: Map<NotificationId, Notification> = registrar
                override fun get(notificationId: NotificationId): Notification? = error("Not yet implemented")
                override fun get(notification: Notification): NotificationId? = error("Not yet implemented")
                override suspend fun register(notification: Notification): NotificationId = error("Not yet implemented")
                override fun unregister(notificationId: NotificationId) = error("Not yet implemented")
                override fun unregister(notification: Notification) = error("Not yet implemented")
            },
            inAppNotificationEventBus = eventBus,
        )
    }
}
