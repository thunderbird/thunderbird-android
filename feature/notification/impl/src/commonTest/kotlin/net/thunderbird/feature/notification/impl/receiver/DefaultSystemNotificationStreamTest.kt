package net.thunderbird.feature.notification.impl.receiver

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.FakeSystemOnlyNotification

class DefaultSystemNotificationStreamTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `notifications should only emit SystemNotification instances`() = runTest {
        // Arrange
        val registry = DefaultNotificationRegistry(dispatcher = UnconfinedTestDispatcher())
        val testSubject = DefaultSystemNotificationStream(registry, mainDispatcher = UnconfinedTestDispatcher())
        val inAppNotification = FakeInAppOnlyNotification()
        val systemNotification = FakeSystemOnlyNotification()
        val inAppAndSystemNotification = FakeNotification()

        testSubject.notifications.test {
            // Assert initial state
            assertThat(awaitItem()).isEmpty()

            // Act
            registry.register(inAppNotification)

            // Assert
            expectNoEvents()

            // Act
            registry.register(systemNotification)

            // Assert
            assertThat(awaitItem()).isEqualTo(setOf(systemNotification))

            // Act
            registry.register(inAppAndSystemNotification)

            // Assert
            assertThat(awaitItem()).isEqualTo(setOf(systemNotification, inAppAndSystemNotification))

            // Act
            registry.unregister(systemNotification)

            // Assert
            assertThat(awaitItem()).isEqualTo(setOf(inAppAndSystemNotification))

            // Act
            registry.unregister(inAppAndSystemNotification)

            // Assert
            assertThat(awaitItem()).isEmpty()
        }
    }
}
