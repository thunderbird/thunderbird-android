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
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry
import net.thunderbird.feature.notification.testing.fake.receiver.FakeSystemNotificationNotifier

@Suppress("MaxLineLength")
class DismissSystemNotificationCommandTest {
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
                notificationRegistry = FakeNotificationRegistry(),
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
                            "${FeatureFlagKey.UseNotificationSenderForSystemNotifications.key} feature flag is not enabled",
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
                notificationRegistry = FakeNotificationRegistry(),
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
                            "${FeatureFlagKey.UseNotificationSenderForSystemNotifications.key} feature flag is not enabled",
                        )
                }
        }

    @Test
    fun `execute should return Success when feature flag Enabled and notification registered`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = FakeNotificationRegistry().apply {
            register(notification)
        }
        val notifier = spy(FakeSystemNotificationNotifier())
        val testSubject = createTestSubject(
            notification = notification,
            notifier = notifier,
            notificationRegistry = registry,
        )
        val expectedId = registry.getValue(notification)

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

        verifySuspend(exactly(1)) { notifier.dismiss(expectedId) }
    }

    @Test
    fun `execute should return Failure when notification not registered`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = FakeNotificationRegistry()
        val notifier = spy(FakeSystemNotificationNotifier())
        val testSubject = createTestSubject(
            notification = notification,
            notifier = notifier,
            notificationRegistry = registry,
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
                    .isInstanceOf<Exception>()
                    .hasMessage("Can't execute command.")
            }

        verifySuspend(exactly(0)) { notifier.dismiss(any()) }
    }

    private fun createTestSubject(
        notification: SystemNotification = FakeNotification(),
        featureFlagProvider: FeatureFlagProvider = FeatureFlagProvider { FeatureFlagResult.Enabled },
        notifier: NotificationNotifier<SystemNotification> = FakeSystemNotificationNotifier(),
        notificationRegistry: NotificationRegistry = FakeNotificationRegistry(),
    ): DismissSystemNotificationCommand {
        val logger = TestLogger()
        return DismissSystemNotificationCommand(
            logger = logger,
            featureFlagProvider = featureFlagProvider,
            notificationRegistry = notificationRegistry,
            notification = notification,
            notifier = notifier,
        )
    }
}
