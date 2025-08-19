package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyles
import net.thunderbird.feature.notification.api.ui.util.printSemanticTree
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.ui.action.createFakeNotificationAction

private const val BUTTON_NOTIFICATION_TEST_TAG = "button_notification_test_tag"

class BannerGlobalNotificationHostTest : ComposeTest() {
    @Test
    fun `should show error banner when severity is fatal`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Fatal,
        )
        mainClock.autoAdvance = false

        setContentWithTheme {
            TestSubject(notification, onActionClick = {})
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

        // Assert
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(contentText)

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
            .assertDoesNotExist()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER)
            .assertDoesNotExist()
    }

    @Test
    fun `should show error banner when severity is critical`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Critical,
        )
        mainClock.autoAdvance = false

        setContentWithTheme {
            TestSubject(notification, onActionClick = {})
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(contentText)

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
            .assertDoesNotExist()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER)
            .assertDoesNotExist()
    }

    @Test
    fun `should show warning banner when severity is warning`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Warning,
        )
        mainClock.autoAdvance = false

        setContentWithTheme {
            TestSubject(notification, onActionClick = {})
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertDoesNotExist()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(contentText)

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER)
            .assertDoesNotExist()
    }

    @Test
    fun `should show info banner when severity is temporary`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Temporary,
        )
        mainClock.autoAdvance = false

        setContentWithTheme {
            TestSubject(notification, onActionClick = {})
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
            .assertDoesNotExist()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER)
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(contentText)
    }

    @Test
    fun `should show info banner when severity is information`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Information,
        )
        mainClock.autoAdvance = false

        setContentWithTheme {
            TestSubject(notification, onActionClick = {})
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
            .assertDoesNotExist()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_INFO_BANNER)
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(contentText)
    }

    @Test
    fun `should show action button when action is not null`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val actionText = "Fake action"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Fatal,
            action = createFakeNotificationAction(title = actionText),
        )
        mainClock.autoAdvance = false

        val actionClicked = mutableStateOf(false)

        setContentWithTheme {
            TestSubject(notification, onActionClick = { actionClicked.value = true })
        }
        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertIsDisplayed()
            .also { printSemanticTree(it) }
            .onChildren()
            .run {
                filterToOne(hasTextExactly(contentText))
                    .assertExists()
                filterToOne(hasTextExactly(actionText))
                    .assertExists()
                    .assert(hasClickAction())
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            }
    }

    @Test
    fun `should not show action button when action is null`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Fatal,
            action = null,
        )
        mainClock.autoAdvance = false

        val actionClicked = mutableStateOf(false)

        setContentWithTheme {
            TestSubject(notification, onActionClick = { actionClicked.value = true })
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()
        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertIsDisplayed()
            .also { printSemanticTree(it) }
            .assert(hasNoClickAction())
            .onChildren()
            .assertCountEquals(1)
            .onFirst()
            .assertTextEquals(contentText)
    }

    @Test
    fun `should call onActionClick when action is clicked`() = runComposeTest {
        // Arrange
        val title = "Notification"
        val contentText = "This is the content text of the notification"
        val actionText = "Fake action"
        val notification = createNotification(
            title = title,
            contentText = contentText,
            severity = NotificationSeverity.Fatal,
            action = createFakeNotificationAction(title = actionText),
        )
        mainClock.autoAdvance = false

        val actionClicked = mutableStateOf(false)

        setContentWithTheme {
            TestSubject(notification, onActionClick = { actionClicked.value = true })
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        printSemanticTree()
        mainClock.advanceTimeBy(1000L)
        printSemanticTree()

        // Assert
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertIsDisplayed()

        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_ERROR_BANNER)
            .assertIsDisplayed()
            .onChildren()
            .filterToOne(hasTextExactly(contentText))

        printSemanticTree()
        onNodeWithText(actionText).performClick()
        assertThat(actionClicked.value)
            .isTrue()
    }

    @Composable
    private fun TestSubject(
        notification: FakeInAppOnlyNotification,
        onActionClick: (NotificationAction) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val state = rememberInAppNotificationHostStateHolder()
        Column(modifier = modifier) {
            ButtonText(
                text = "Trigger Notification",
                onClick = { state.showInAppNotification(notification) },
                modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
            )

            BannerGlobalNotificationHost(
                hostStateHolder = state,
                onActionClick = onActionClick,
            )
        }
    }

    private fun createNotification(
        title: String,
        contentText: String,
        severity: NotificationSeverity,
        action: NotificationAction? = null,
    ) = FakeInAppOnlyNotification(
        title = title,
        contentText = contentText,
        severity = severity,
        inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
        actions = action?.let { setOf(it) }.orEmpty(),
    )
}
