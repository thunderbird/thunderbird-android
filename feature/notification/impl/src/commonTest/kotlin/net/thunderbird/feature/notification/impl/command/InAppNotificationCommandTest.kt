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
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry
import net.thunderbird.feature.notification.testing.fake.receiver.FakeInAppNotificationNotifier

@Suppress("MaxLineLength")
class InAppNotificationCommandTest {
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
    fun `execute should return Success when display_in_app_notifications feature flag is Enabled`() =
        runTest {
            // Arrange
            val notification = FakeNotification(
                severity = NotificationSeverity.Information,
            )
            val notifier = spy(FakeInAppNotificationNotifier())
            val testSubject = createTestSubject(
                notification = notification,
                notifier = notifier,
            )

            // Act
            val outcome = testSubject.execute()

            // Assert
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<Success<InAppNotification>>>()
                .prop("data") { it.data }
                .all {
                    prop(Success<InAppNotification>::command)
                        .isEqualTo(testSubject)
                }

            verifySuspend(exactly(1)) {
                notifier.show(id = any(), notification)
            }
        }

    private fun createTestSubject(
        notification: InAppNotification = FakeNotification(),
        featureFlagProvider: FeatureFlagProvider = FeatureFlagProvider { FeatureFlagResult.Enabled },
        notifier: NotificationNotifier<InAppNotification> = FakeInAppNotificationNotifier(),
        notificationRegistry: NotificationRegistry = FakeNotificationRegistry(),
    ): InAppNotificationCommand {
        val logger = TestLogger()
        return InAppNotificationCommand(
            logger = logger,
            featureFlagProvider = featureFlagProvider,
            notificationRegistry = notificationRegistry,
            notification = notification,
            notifier = notifier,
        )
    }
}
