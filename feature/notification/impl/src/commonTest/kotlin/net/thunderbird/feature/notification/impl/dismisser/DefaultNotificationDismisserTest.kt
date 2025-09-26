package net.thunderbird.feature.notification.impl.dismisser

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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.command.DismissInAppNotificationCommand
import net.thunderbird.feature.notification.impl.command.DismissSystemNotificationCommand
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry
import net.thunderbird.feature.notification.testing.fake.FakeSystemOnlyNotification
import net.thunderbird.feature.notification.testing.fake.receiver.FakeInAppNotificationNotifier
import net.thunderbird.feature.notification.testing.fake.receiver.FakeSystemNotificationNotifier

@Suppress("MaxLineLength")
class DefaultNotificationDismisserTest {

    @Test
    fun `dismiss(id) should emit Failure when notification is not found`() = runTest {
        // Arrange
        val registry = FakeNotificationRegistry()
        val dismisser = createTestSubject(notificationRegistry = registry)
        val missingId = NotificationId(value = Random.nextInt())

        // Act
        val outcomes = dismisser.dismiss(missingId).toList(mutableListOf())

        // Assert
        assertThat(outcomes.single())
            .isInstanceOf<Outcome.Failure<Failure<Notification>>>()
            .prop("error") { it.error }
            .all {
                prop(Failure<Notification>::command).isEqualTo(null)
                prop(Failure<Notification>::throwable)
                    .isInstanceOf<NotificationCommandException>()
                    .hasMessage("Notification with id '$missingId' not found")
            }
    }

    @Test
    fun `dismiss(notification) should emit Success and call system notifier when registered SystemNotification`() = runTest {
        // Arrange
        val registry = FakeNotificationRegistry()
        val systemNotifier = spy(FakeSystemNotificationNotifier())
        val inAppNotifier = spy(FakeInAppNotificationNotifier())
        val dismisser = createTestSubject(
            notificationRegistry = registry,
            systemNotificationNotifier = systemNotifier,
            inAppNotificationNotifier = inAppNotifier,
        )
        val notification: SystemNotification = FakeSystemOnlyNotification()
        // register notification to be dismissible
        registry.register(notification)

        // Act
        val outcomes = dismisser.dismiss(notification).toList(mutableListOf())

        // Assert
        assertThat(outcomes.single())
            .isInstanceOf<Outcome.Success<Success<Notification>>>()
            .prop("data") { it.data }
            .prop(Success<Notification>::command)
            .isInstanceOf<DismissSystemNotificationCommand>()
        verifySuspend(exactly(1)) { systemNotifier.dismiss(id = any()) }
        verifySuspend(exactly(0)) { inAppNotifier.dismiss(id = any()) }
    }

    @Test
    fun `dismiss(notification) should emit Success and call in-app notifier when registered InAppNotification`() = runTest {
        // Arrange
        val registry = FakeNotificationRegistry()
        val systemNotifier = spy(FakeSystemNotificationNotifier())
        val inAppNotifier = spy(FakeInAppNotificationNotifier())
        val dismisser = createTestSubject(
            notificationRegistry = registry,
            systemNotificationNotifier = systemNotifier,
            inAppNotificationNotifier = inAppNotifier,
        )
        val notification: InAppNotification = FakeInAppOnlyNotification()
        registry.register(notification)

        // Act
        val outcomes = dismisser.dismiss(notification).toList(mutableListOf())

        // Assert
        assertThat(outcomes.single())
            .isInstanceOf<Outcome.Success<Success<Notification>>>()
            .prop("data") { it.data }
            .prop(Success<Notification>::command)
            .isInstanceOf<DismissInAppNotificationCommand>()
        verifySuspend(exactly(1)) { inAppNotifier.dismiss(id = any()) }
        verifySuspend(exactly(0)) { systemNotifier.dismiss(id = any()) }
    }

    @Test
    fun `dismiss(notification) should emit Failure when notification is not registered`() = runTest {
        // Arrange
        val registry = FakeNotificationRegistry() // empty
        val dismisser = createTestSubject(notificationRegistry = registry)
        val unregistered: Notification = FakeNotification()

        // Act
        val outcomes = dismisser.dismiss(unregistered).toList(mutableListOf())

        // Assert
        assertThat(outcomes.single())
            .isInstanceOf<Outcome.Failure<Failure<Notification>>>()
            .prop("error") { it.error }
            .all {
                prop(Failure<Notification>::command).isEqualTo(null)
                prop(Failure<Notification>::throwable)
                    .isInstanceOf<NotificationCommandException>()
                    .hasMessage("Can't dismiss notification that is already dismissed")
            }
    }

    private fun createTestSubject(
        logger: TestLogger = TestLogger(),
        featureFlagProvider: FeatureFlagProvider = FeatureFlagProvider { FeatureFlagResult.Enabled },
        notificationRegistry: NotificationRegistry = FakeNotificationRegistry(),
        systemNotificationNotifier: NotificationNotifier<SystemNotification> = FakeSystemNotificationNotifier(),
        inAppNotificationNotifier: NotificationNotifier<InAppNotification> = FakeInAppNotificationNotifier(),
    ): DefaultNotificationDismisser {
        return DefaultNotificationDismisser(
            logger = logger,
            featureFlagProvider = featureFlagProvider,
            notificationRegistry = notificationRegistry,
            systemNotificationNotifier = systemNotificationNotifier,
            inAppNotificationNotifier = inAppNotificationNotifier,
        )
    }
}
