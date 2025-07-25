package net.thunderbird.feature.notification.api.sender.compat

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.api.fake.FakeNotification
import net.thunderbird.feature.notification.api.fake.FakeSystemOnlyNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.api.sender.NotificationSender

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
        ).toTypedArray()
        val sender = FakeNotificationSender(results = expectedResults.asFlow())
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
        assertThat(actualResults).containsExactlyInAnyOrder(elements = expectedResults)
    }

    private class FakeNotificationSender(
        private val results: Flow<Outcome<Success<Notification>, Failure<Notification>>>,
    ) : NotificationSender {
        override fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> =
            results
    }

    private class FakeInAppNotificationNotifier : NotificationNotifier<InAppNotification> {
        override suspend fun show(
            id: NotificationId,
            notification: InAppNotification,
        ) = Unit

        override fun dispose() = Unit
    }

    private class FakeInAppNotificationCommand(
        notification: InAppNotification = FakeInAppOnlyNotification(),
        notifier: NotificationNotifier<InAppNotification> = FakeInAppNotificationNotifier(),
    ) : NotificationCommand<InAppNotification>(notification, notifier) {
        override suspend fun execute(): Outcome<Success<InAppNotification>, Failure<InAppNotification>> =
            error("not implemented")
    }

    private class FakeSystemNotificationNotifier : NotificationNotifier<SystemNotification> {
        override suspend fun show(
            id: NotificationId,
            notification: SystemNotification,
        ) = Unit

        override fun dispose() = Unit
    }

    private class FakeSystemNotificationCommand(
        notification: SystemNotification = FakeSystemOnlyNotification(),
        notifier: NotificationNotifier<SystemNotification> = FakeSystemNotificationNotifier(),
    ) : NotificationCommand<SystemNotification>(notification, notifier) {
        override suspend fun execute(): Outcome<Success<SystemNotification>, Failure<SystemNotification>> =
            error("not implemented")
    }
}
