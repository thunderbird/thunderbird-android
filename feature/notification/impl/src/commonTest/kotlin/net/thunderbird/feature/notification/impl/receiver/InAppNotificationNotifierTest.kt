package net.thunderbird.feature.notification.impl.receiver

import assertk.assertThat
import assertk.assertions.isEqualTo
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
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry

class InAppNotificationNotifierTest {
    @Test
    fun `show should not register notification when it is already present in NotificationRegistry`() = runTest {
        // Arrange
        val expectedNotificationId = NotificationId(value = 1)
        val notification = FakeInAppOnlyNotification()
        val registrar = mapOf(expectedNotificationId to notification)
        val registry = spy(FakeNotificationRegistry(initialRegistrar = registrar))
        val testSubject = createTestSubject(registry)

        // Act
        val actual = testSubject.show(notification)

        // Assert
        verifySuspend(exactly(0)) { registry.register(any()) }
        assertThat(actual).isEqualTo(expectedNotificationId)
    }

    @Test
    fun `show should register notification when it is not present in NotificationRegistry`() = runTest {
        // Arrange
        val notification = FakeInAppOnlyNotification()
        val registrarInitialSize = 100
        val registrar = buildMap<NotificationId, Notification> {
            repeat(times = registrarInitialSize) { index ->
                put(NotificationId(index), FakeInAppOnlyNotification(title = "fake title $index"))
            }
        }
        val registry = spy(
            FakeNotificationRegistry(
                initialRegistrar = registrar,
                useRandomIdForRegistering = false,
            ),
        )
        val testSubject = createTestSubject(registry)

        // Act
        val id = testSubject.show(notification)

        // Assert
        verifySuspend(exactly(1)) { registry.register(notification) }
        assertThat(id)
            .isEqualTo(NotificationId(registrarInitialSize + 1))
    }

    private fun createTestSubject(
        registry: NotificationRegistry = FakeNotificationRegistry(),
    ): InAppNotificationNotifier {
        return InAppNotificationNotifier(
            logger = TestLogger(),
            notificationRegistry = registry,
        )
    }
}
