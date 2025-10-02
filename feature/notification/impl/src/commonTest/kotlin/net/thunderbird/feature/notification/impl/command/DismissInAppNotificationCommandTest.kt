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
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry
import net.thunderbird.feature.notification.testing.fake.receiver.FakeInAppNotificationNotifier

class DismissInAppNotificationCommandTest {
    @Test
    fun `execute should return Failure when display_in_app_notifications feature flag is Disabled`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject(
                featureFlagProvider = { key ->
                    when (key) {
                        FeatureFlagKey.DisplayInAppNotifications -> FeatureFlagResult.Disabled
                        else -> FeatureFlagResult.Enabled
                    }
                },
                notificationRegistry = FakeNotificationRegistry(),
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<Failure<InAppNotification>>>()
                .prop("error") { it.error }
                .all {
                    prop(Failure<InAppNotification>::command)
                        .isEqualTo(testSubject)
                    prop(Failure<InAppNotification>::throwable)
                        .isInstanceOf<NotificationCommandException>()
                        .hasMessage(
                            "${FeatureFlagKey.DisplayInAppNotifications.key} feature flag is not enabled",
                        )
                }
        }

    @Test
    fun `execute should return Failure when display_in_app_notifications feature flag is Unavailable`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject(
                featureFlagProvider = { key ->
                    when (key) {
                        FeatureFlagKey.DisplayInAppNotifications -> FeatureFlagResult.Unavailable
                        else -> FeatureFlagResult.Enabled
                    }
                },
                notificationRegistry = FakeNotificationRegistry(),
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<Failure<InAppNotification>>>()
                .prop("error") { it.error }
                .all {
                    prop(Failure<InAppNotification>::command)
                        .isEqualTo(testSubject)
                    prop(Failure<InAppNotification>::throwable)
                        .isInstanceOf<NotificationCommandException>()
                        .hasMessage(
                            "${FeatureFlagKey.DisplayInAppNotifications.key} feature flag is not enabled",
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
        val notifier = spy(FakeInAppNotificationNotifier())
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
            .isInstanceOf<Outcome.Success<Success<InAppNotification>>>()
            .prop("data") { it.data }
            .all {
                prop(Success<InAppNotification>::command)
                    .isEqualTo(testSubject)
                prop(Success<InAppNotification>::notificationId)
                    .isEqualTo(expectedId)
            }

        verifySuspend(exactly(1)) { notifier.dismiss(expectedId) }
    }

    @Test
    fun `execute should return Failure when notification not registered`() = runTest {
        // Arrange
        val notification = FakeNotification()
        val registry = FakeNotificationRegistry() // empty, not registered
        val notifier = spy(FakeInAppNotificationNotifier())
        val testSubject = createTestSubject(
            notification = notification,
            notifier = notifier,
            notificationRegistry = registry,
        )

        // Act
        val outcome = testSubject.execute()

        // Assert
        assertThat(outcome)
            .isInstanceOf<Outcome.Failure<Failure<InAppNotification>>>()
            .prop("error") { it.error }
            .all {
                prop(Failure<InAppNotification>::command)
                    .isEqualTo(testSubject)
                prop(Failure<InAppNotification>::throwable)
                    .isInstanceOf<Exception>()
                    .hasMessage("Can't execute command.")
            }

        verifySuspend(exactly(0)) { notifier.dismiss(any()) }
    }

    private fun createTestSubject(
        notification: InAppNotification = FakeNotification(),
        featureFlagProvider: FeatureFlagProvider = FeatureFlagProvider { FeatureFlagResult.Enabled },
        notifier: NotificationNotifier<InAppNotification> = FakeInAppNotificationNotifier(),
        notificationRegistry: NotificationRegistry = FakeNotificationRegistry(),
    ): DismissInAppNotificationCommand {
        val logger = TestLogger()
        return DismissInAppNotificationCommand(
            logger = logger,
            featureFlagProvider = featureFlagProvider,
            notificationRegistry = notificationRegistry,
            notification = notification,
            notifier = notifier,
        )
    }
}
