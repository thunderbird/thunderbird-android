package net.thunderbird.feature.notification.impl

import assertk.assertThat
import assertk.assertions.containsAtLeast
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.testing.fake.FakeNotification

@Suppress("MaxLineLength")
class DefaultNotificationRegistryTest {
    @Test
    fun `register should return NotificationId given notification`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = DefaultNotificationRegistry()

        // Act
        val notificationId = registry.register(notification)

        // Assert
        assertThat(registry[notificationId])
            .isNotNull()
            .isEqualTo(notification)
    }

    @Test
    fun `register should return same NotificationId when registering the same notification multiple times`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = DefaultNotificationRegistry()

        // Act
        val notificationId1 = registry.register(notification)
        val notificationId2 = registry.register(notification)

        // Assert
        assertThat(notificationId1)
            .isEqualTo(notificationId2)
        assertThat(registry[notificationId1])
            .isNotNull()
            .isEqualTo(notification)
        assertThat(registry[notificationId2])
            .isNotNull()
            .isEqualTo(notification)
    }

    @Test
    fun `register should not register duplicated notifications when running concurrently`() = runTest {
        // Arrange
        val notificationSize = 100
        val registerTries = 50
        val notifications = List(size = notificationSize) { index ->
            FakeNotification(
                title = "fake notification $index",
            )
        }
        val expectedNotificationIds = List(size = notificationSize) { index ->
            NotificationId(value = index + 1)
        }
        val registry = DefaultNotificationRegistry()

        // Act
        List(size = registerTries) {
            thread(start = true) {
                notifications.forEach { notification ->
                    runBlocking {
                        registry.register(notification)
                    }
                }
            }
        }.forEach {
            it.join()
        }

        // Assert
        val registrar = registry.registrar
        assertThat(registrar).hasSize(notificationSize)
        assertThat(registrar)
            .containsAtLeast(elements = expectedNotificationIds.zip(notifications).toTypedArray())
    }

    @Test
    fun `operator get Notification should return NotificationId when notification is in the registrar`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = DefaultNotificationRegistry()
        registry.register(notification)

        // Act
        val notificationId = registry[notification]

        // Assert
        assertThat(notificationId).isNotNull()
    }

    @Test
    fun `operator get Notification should return null when notification is NOT in the registrar`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val notRegisteredNotification = FakeNotification(title = "that is not registered!!")
        val registry = DefaultNotificationRegistry()
        registry.register(notification)

        // Act
        val notificationId = registry[notRegisteredNotification]

        // Assert
        assertThat(notificationId).isNull()
    }

    @Test
    fun `operator get NotificationId should return Notification when notification is in the registrar`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = DefaultNotificationRegistry()
        val notificationId = registry.register(notification)

        // Act
        val registrarNotification = registry[notificationId]

        // Assert
        assertThat(registrarNotification).isNotNull()
    }

    @Test
    fun `operator get NotificationId should return null when notification is NOT in the registrar`() = runTest {
        // Arrange
        val registry = DefaultNotificationRegistry()
        val notification = FakeNotification()
        registry.register(notification)

        // Act
        val notificationId = registry[NotificationId(value = Int.MAX_VALUE)]

        // Assert
        assertThat(notificationId).isNull()
    }

    @Test
    fun `unregister should remove notification from registrar when given a notification object and Notification is in registrar`() =
        runTest {
            // Arrange
            val registry = DefaultNotificationRegistry()
            val notification = FakeNotification()
            registry.register(notification)

            // Act
            registry.unregister(notification)

            // Assert
            assertThat(registry[notification]).isNull()
        }

    @Test
    fun `unregister should remove notification from registrar when given a notification id and Notification is in registrar`() =
        runTest {
            // Arrange
            val registry = DefaultNotificationRegistry()
            val notification = FakeNotification()
            val notificationId = registry.register(notification)

            // Act
            registry.unregister(notificationId)

            // Assert
            assertThat(registry[notification]).isNull()
        }
}
