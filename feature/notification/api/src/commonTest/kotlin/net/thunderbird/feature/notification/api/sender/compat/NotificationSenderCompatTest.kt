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
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.command.outcome.CommandExecutionFailed
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.command.outcome.Success
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
        val expectedResults = listOf<NotificationCommandOutcome<Notification>>(
            Outcome.success(Success(NotificationId(1), FakeInAppNotificationCommand())),
            Outcome.success(Success(NotificationId(1), FakeSystemNotificationCommand())),
            Outcome.failure(
                error = CommandExecutionFailed(
                    command = FakeSystemNotificationCommand(),
                    throwable = NotificationCommandException("What an issue?"),
                ),
            ),
        )
        val sender = FakeNotificationSender(results = expectedResults)
        val actualResults = mutableListOf<NotificationCommandOutcome<Notification>>()
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
