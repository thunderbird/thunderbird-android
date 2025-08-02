package net.thunderbird.feature.notification.api.sender.compat

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.command.FakeInAppNotificationCommand
import net.thunderbird.feature.notification.testing.fake.command.FakeSystemNotificationCommand
import net.thunderbird.feature.notification.testing.fake.sender.FakeNotificationSender

class NotificationSenderCompatTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send should call listener callback whenever a result is received`() {
        // Arrange
        val expectedResults = listOf<Outcome<Success<Notification>, Failure<Notification>>>(
            Outcome.success(Success(FakeInAppNotificationCommand())),
            Outcome.success(Success(FakeSystemNotificationCommand())),
            Outcome.failure(
                error = Failure(
                    command = FakeSystemNotificationCommand(),
                    throwable = NotificationCommandException("What an issue?"),
                ),
            ),
        )
        val sender = FakeNotificationSender(results = expectedResults)
        val actualResults = mutableListOf<Outcome<Success<Notification>, Failure<Notification>>>()
        val listener = spy(
            NotificationSenderCompat.OnResultListener { outcome ->
                actualResults += outcome
            },
        )
        val testSubject = NotificationSenderCompat(
            notificationSender = sender,
            mainImmediateDispatcher = UnconfinedTestDispatcher(),
        )

        // Act
        testSubject.send(notification = FakeNotification(), listener)

        // Assert
        verify(exactly(expectedResults.size)) {
            listener.onResult(outcome = any())
        }
        assertThat(actualResults).containsExactlyInAnyOrder(elements = expectedResults.toTypedArray())
    }
}
