package net.thunderbird.feature.notification.impl.command

import assertk.all
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.fake.FakeNotification
import net.thunderbird.feature.notification.impl.fake.FakeSystemOnlyNotification

@Suppress("MaxLineLength")
class SystemNotificationCommandTest {
    @Test
    fun `execute should return Failure when use_notification_sender_for_system_notifications feature flag is Disabled`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject(
                featureFlagProvider = { key ->
                    when (key) {
                        FeatureFlagKey.UseNotificationSenderForSystemNotifications -> FeatureFlagResult.Disabled
                        else -> FeatureFlagResult.Enabled
                    }
                },
            )

            // Act
            val outcome = testSubject.execute()

            // Assert

            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<Failure<SystemNotification>>>()
                .prop("error") { it.error }
                .all {
                    prop(Failure<SystemNotification>::command)
                        .isEqualTo(testSubject)
                    prop(Failure<SystemNotification>::throwable)
                        .isInstanceOf<NotificationCommandException>()
                        .hasMessage(
                            "${FeatureFlagKey.UseNotificationSenderForSystemNotifications.key} feature flag" +
                                "is not enabled",
                        )
                }
        }

    @Test
    fun `execute should return Failure when use_notification_sender_for_system_notifications feature flag is Unavailable`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject(
                featureFlagProvider = { key ->
                    when (key) {
                        FeatureFlagKey.UseNotificationSenderForSystemNotifications -> FeatureFlagResult.Unavailable
                        else -> FeatureFlagResult.Enabled
                    }
                },
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<Failure<SystemNotification>>>()
                .prop("error") { it.error }
                .all {
                    prop(Failure<SystemNotification>::command)
                        .isEqualTo(testSubject)
                    prop(Failure<SystemNotification>::throwable)
                        .isInstanceOf<NotificationCommandException>()
                        .hasMessage(
                            "${FeatureFlagKey.UseNotificationSenderForSystemNotifications.key} feature flag" +
                                "is not enabled",
                        )
                }
        }

    @Test
    fun `execute should return Failure when the app is in the foreground, notification is also InApp and severity is not Fatal or Critical`() =
        runTest {
            // Arrange
            val notification = FakeNotification(
                severity = NotificationSeverity.Information,
            )
            val testSubject = createTestSubject(
                notification = notification,
                // TODO(#9391): Verify if the app is backgrounded.
                isAppInBackground = { false },
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<Failure<SystemNotification>>>()
                .prop("error") { it.error }
                .all {
                    prop(Failure<SystemNotification>::command)
                        .isEqualTo(testSubject)
                    prop(Failure<SystemNotification>::throwable)
                        .isInstanceOf<NotificationCommandException>()
                        .hasMessage("Can't execute command.")
                }
        }

    @Test
    fun `execute should return Success when the app is in the background`() =
        runTest {
            // Arrange
            val notification = FakeNotification(
                severity = NotificationSeverity.Information,
            )
            val notifier = spy(FakeNotifier())
            val testSubject = createTestSubject(
                notification = notification,
                // TODO(#9391): Verify if the app is backgrounded.
                isAppInBackground = { true },
                notifier = notifier,
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<Success<SystemNotification>>>()
                .prop("data") { it.data }
                .all {
                    prop(Success<SystemNotification>::command)
                        .isEqualTo(testSubject)
                }

            verifySuspend(exactly(1)) {
                notifier.show(any(), notification)
            }
        }

    @Test
    fun `execute should return Success when the notification severity is Fatal`() =
        runTest {
            // Arrange
            val notification = FakeNotification(
                severity = NotificationSeverity.Fatal,
            )
            val notifier = spy(FakeNotifier())
            val testSubject = createTestSubject(
                notification = notification,
                // TODO(#9391): Verify if the app is backgrounded.
                isAppInBackground = { false },
                notifier = notifier,
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<Success<SystemNotification>>>()
                .prop("data") { it.data }
                .all {
                    prop(Success<SystemNotification>::command)
                        .isEqualTo(testSubject)
                }

            verifySuspend(exactly(1)) {
                notifier.show(any(), notification)
            }
        }

    @Test
    fun `execute should return Success when the notification severity is Critical`() =
        runTest {
            // Arrange
            val notification = FakeNotification(
                severity = NotificationSeverity.Critical,
            )
            val notifier = spy(FakeNotifier())
            val testSubject = createTestSubject(
                notification = notification,
                // TODO(#9391): Verify if the app is backgrounded.
                isAppInBackground = { false },
                notifier = notifier,
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<Success<SystemNotification>>>()
                .prop("data") { it.data }
                .all {
                    prop(Success<SystemNotification>::command)
                        .isEqualTo(testSubject)
                }

            verifySuspend(exactly(1)) {
                notifier.show(any(), notification)
            }
        }

    @Test
    fun `execute should return Success when the notification the app is not in background and notification is not an in-app notification`() =
        runTest {
            // Arrange
            val notification = FakeSystemOnlyNotification(
                severity = NotificationSeverity.Information,
            )
            val notifier = spy(FakeNotifier())
            val testSubject = createTestSubject(
                notification = notification,
                // TODO(#9391): Verify if the app is backgrounded.
                isAppInBackground = { false },
                notifier = notifier,
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<Success<SystemNotification>>>()
                .prop("data") { it.data }
                .all {
                    prop(Success<SystemNotification>::command)
                        .isEqualTo(testSubject)
                }

            verifySuspend(exactly(1)) {
                notifier.show(any(), notification)
            }
        }

    private fun createTestSubject(
        notification: SystemNotification = FakeNotification(),
        featureFlagProvider: FeatureFlagProvider = FeatureFlagProvider { FeatureFlagResult.Enabled },
        notifier: NotificationNotifier<SystemNotification> = FakeNotifier(),
        notificationRegistry: NotificationRegistry = FakeNotificationRegistry(),
        isAppInBackground: () -> Boolean = {
            // TODO(#9391): Verify if the app is backgrounded.
            false
        },
    ): SystemNotificationCommand {
        val logger = TestLogger()
        return SystemNotificationCommand(
            logger = logger,
            featureFlagProvider = featureFlagProvider,
            notificationRegistry = notificationRegistry,
            notification = notification,
            notifier = notifier,
            isAppInBackground = isAppInBackground,
        )
    }
}

private open class FakeNotificationRegistry : NotificationRegistry {
    override val registrar: Map<NotificationId, Notification>
        get() = TODO("Not yet implemented")

    override fun get(notificationId: NotificationId): Notification? {
        TODO("Not yet implemented")
    }

    override fun get(notification: Notification): NotificationId? {
        TODO("Not yet implemented")
    }

    override suspend fun register(notification: Notification): NotificationId {
        return NotificationId(value = Random.nextInt())
    }

    override fun unregister(notificationId: NotificationId) {
        TODO("Not yet implemented")
    }

    override fun unregister(notification: Notification) {
        TODO("Not yet implemented")
    }
}

private open class FakeNotifier : NotificationNotifier<SystemNotification> {
    override suspend fun show(
        id: NotificationId,
        notification: SystemNotification,
    ) = Unit

    override fun dispose() = Unit
}
