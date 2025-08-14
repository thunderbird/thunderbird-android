package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
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
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostState
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyles
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.ui.action.createFakeNotificationAction

const val BUTTON_NOTIFICATION_TEST_TAG = "button_notification_test_tag"

class BannerGlobalNotificationHostTest : ComposeTest() {
    @Test
    fun `should show error banner when severity is fatal`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Fatal,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(),
            )
            mainClock.autoAdvance = false

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = {},
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(contentText)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.WARNING_BANNER_TEST_TAG)
                .assertDoesNotExist()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.INFO_BANNER_TEST_TAG)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `should show error banner when severity is critical`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Critical,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(),
            )
            mainClock.autoAdvance = false

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = {},
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(contentText)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.WARNING_BANNER_TEST_TAG)
                .assertDoesNotExist()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.INFO_BANNER_TEST_TAG)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `should show warning banner when severity is warning`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Warning,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(),
            )
            mainClock.autoAdvance = false

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = {},
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
                .assertDoesNotExist()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.WARNING_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(contentText)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.INFO_BANNER_TEST_TAG)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `should show info banner when severity is temporary`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Temporary,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(),
            )
            mainClock.autoAdvance = false

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = {},
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.WARNING_BANNER_TEST_TAG)
                .assertDoesNotExist()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.INFO_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(contentText)
        }
    }

    @Test
    fun `should show info banner when severity is information`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Information,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(),
            )
            mainClock.autoAdvance = false

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = {},
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.WARNING_BANNER_TEST_TAG)
                .assertDoesNotExist()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.INFO_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(contentText)
        }
    }

    @Test
    fun `should show action button when action is not null`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val actionText = "Fake action"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Fatal,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(
                    createFakeNotificationAction(title = actionText),
                ),
            )
            mainClock.autoAdvance = false

            val actionClicked = mutableStateOf(false)

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = { actionClicked.value = true },
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
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
    }

    @Test
    fun `should not show action button when action is null`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Fatal,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(), // Empty set will create a null Action on BannerGlobalVisual
            )
            mainClock.autoAdvance = false

            val actionClicked = mutableStateOf(false)

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = { actionClicked.value = true },
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .also { printSemanticTree(it) }
                .assert(hasNoClickAction())
                .onChildren()
                .assertCountEquals(1)
                .onFirst()
                .assertTextEquals(contentText)
        }
    }

    @Test
    fun `should call onActionClick when action is clicked`() = runComposeTest {
        composeTestRule.run {
            // Arrange
            val title = "Notification"
            val contentText = "This is the content text of the notification"
            val actionText = "Fake action"
            val notification = FakeInAppOnlyNotification(
                title = title,
                contentText = contentText,
                severity = NotificationSeverity.Fatal,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(
                    createFakeNotificationAction(title = actionText),
                ),
            )
            mainClock.autoAdvance = false

            val actionClicked = mutableStateOf(false)

            setContentWithTheme {
                val state = rememberInAppNotificationHostState()
                Column {
                    ButtonText(
                        text = "Trigger Notification",
                        onClick = { state.showInAppNotification(notification) },
                        modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                    )

                    // Act
                    BannerGlobalNotificationHost(
                        hostStateHolder = state,
                        onActionClick = { actionClicked.value = true },
                    )
                }
            }

            // Assert
            printSemanticTree()

            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            printSemanticTree()

            mainClock.advanceTimeBy(1000L)

            onNodeWithTag(BannerGlobalNotificationHostDefaults.HOST_TEST_TAG)
                .assertIsDisplayed()

            onNodeWithTag(BannerGlobalNotificationHostDefaults.ERROR_BANNER_TEST_TAG)
                .assertIsDisplayed()
                .onChildren()
                .filterToOne(hasTextExactly(contentText))

            printSemanticTree()
            onNodeWithText(actionText).performClick()
            assertThat(actionClicked.value)
                .isTrue()
        }
    }

    private fun printSemanticTree(root: SemanticsNodeInteraction = composeTestRule.onRoot(useUnmergedTree = true)) {
        println()
        println("Semantic tree:")
        println(root.printToString())
        println()
    }
}
