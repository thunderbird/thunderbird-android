package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults.TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyles
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.ui.action.createFakeNotificationAction
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

private const val BUTTON_NOTIFICATION_TEST_TAG = "button_notification_test_tag"

@Suppress("MaxLineLength")
class BannerInlineNotificationListHostTest : ComposeTest() {
    @Test
    fun `should display banner inline notification list`() = runComposeTest {
        // Arrange
        val title = "Notification in test"
        val supportingText = "The supporting text"
        val action = createFakeNotificationAction(title = "Action 1")
        val notification = createNotification(title = title, supportingText = supportingText, actions = setOf(action))
        mainClock.autoAdvance = false
        setContentWithTheme {
            Column {
                val state = rememberInAppNotificationHostStateHolder()
                ButtonText(
                    text = "Trigger Notification",
                    onClick = {
                        state.showInAppNotification(notification)
                    },
                    modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
                )
                BannerInlineNotificationListHost(
                    hostStateHolder = state,
                    onActionClick = { },
                    onOpenErrorNotificationsClick = { },
                )
            }
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

        // Advance Animation
        mainClock.advanceTimeBy(10000L)

        // Assert
        printSemanticTree()
        onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
            .assertIsDisplayed()

        val listHost = onNodeWithTag(
            BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
            useUnmergedTree = true,
        ).assertIsDisplayed()

        listHost
            .onChildren()
            .assertCountEquals(1)

        listHost.assertBannerInline(
            index = 0,
            title = title,
            supportingText = supportingText,
            assertActions = {
                assertCountEquals(1)
                val actionButton = filterToOne(
                    matcher = SemanticsMatcher.expectValue(
                        key = SemanticsProperties.Role,
                        expectedValue = Role.Button,
                    ) and hasClickAction(),
                ).assertIsDisplayed()

                actionButton
                    .onChildren()
                    .filterToOne(hasTextExactly(action.title))
                    .assertIsDisplayed()
            },
        )

        onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_CHECK_ERROR_NOTIFICATIONS)
            .assertIsNotDisplayed()
    }

    @Test
    fun `should display banner inline notification list with all banners when there are 2 notifications`() =
        runComposeTest {
            // Arrange
            val notification1 = createNotification(title = "Notification 1", supportingText = "The supporting text")
            val notification2 = createNotification(title = "Notification 2", supportingText = "The supporting text")
            val notifications = listOf(notification1, notification2)

            mainClock.autoAdvance = false
            setContentWithPreviewAndResources {
                TestSubject(notifications = notifications)
            }

            // Act
            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            // Advance Animation
            mainClock.advanceTimeBy(1000L)

            // Assert
            printSemanticTree()
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            val listHost = onNodeWithTag(
                BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
                useUnmergedTree = true,
            ).assertIsDisplayed()

            listHost
                .onChildren()
                .assertCountEquals(2)

            listHost.assertBannerInline(
                index = 0,
                title = notification1.title,
                supportingText = requireNotNull(notification1.contentText),
                assertActions = { assertCountEquals(2) },
            )

            listHost.assertBannerInline(
                index = 1,
                title = notification2.title,
                supportingText = requireNotNull(notification2.contentText),
                assertActions = { assertCountEquals(2) },
            )
        }

    @Test
    fun `should display banner inline notification list with check notification banner when there are more than 2 notifications`() =
        runComposeTest {
            // Arrange
            val notification1 = createNotification(title = "Notification 1", supportingText = "The supporting text")
            val notification2 = createNotification(title = "Notification 2", supportingText = "The supporting text")
            val notification3 = createNotification(title = "Notification 3", supportingText = "The supporting text")
            val notifications = listOf(notification1, notification2, notification3)
            mainClock.autoAdvance = false
            setContentWithPreviewAndResources {
                TestSubject(notifications = notifications)
            }

            // Act
            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()

            // Advance Animation
            mainClock.advanceTimeBy(1000L)

            // Assert
            printSemanticTree()
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            val listHost = onNodeWithTag(
                BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
                useUnmergedTree = true,
            ).assertIsDisplayed()

            listHost
                .onChildren()
                .assertCountEquals(2)

            listHost.assertBannerInline(
                index = 0,
                title = notification1.title,
                supportingText = requireNotNull(notification1.contentText),
                assertActions = { assertCountEquals(2) },
            )

            listHost.assertBannerInline(
                index = 1,
                title = "Check Error Notifications",
                supportingText = "Some messages need your attention.",
                assertActions = {
                    assertCountEquals(1)
                    val actionButton = filterToOne(
                        matcher = SemanticsMatcher.expectValue(
                            key = SemanticsProperties.Role,
                            expectedValue = Role.Button,
                        ) and hasClickAction(),
                    ).assertIsDisplayed()

                    actionButton
                        .onChildren()
                        .filterToOne(hasTextExactly("Open notifications"))
                        .assertIsDisplayed()
                },
            )
        }

    @Test
    fun `should trigger onActionClick when action button is clicked`() = runComposeTest {
        // Arrange
        val title = "Notification in test"
        val supportingText = "The supporting text"
        val action = createFakeNotificationAction(title = "Action 1")
        val notification = createNotification(title = title, supportingText = supportingText, actions = setOf(action))
        mainClock.autoAdvance = false
        val actionClicked = mutableStateOf<NotificationAction?>(value = null)
        setContentWithTheme {
            TestSubject(
                notifications = listOf(notification),
                onActionClick = { notification -> actionClicked.value = notification },
            )
        }

        // Act
        onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
        // Advance Animation
        mainClock.advanceTimeBy(1000L)
        onNodeWithText(action.title).performClick()

        // Assert
        printSemanticTree()
        onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
            .assertIsDisplayed()

        val listHost = onNodeWithTag(
            BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
            useUnmergedTree = true,
        ).assertIsDisplayed()

        listHost
            .onChildren()
            .assertCountEquals(1)

        listHost.assertBannerInline(
            index = 0,
            title = title,
            supportingText = supportingText,
            assertActions = { assertCountEquals(1) },
        )

        assertThat(actionClicked.value)
            .isEqualTo(action)
    }

    @Test
    fun `should trigger onOpenErrorNotificationsClick when open error notifications button is clicked`() =
        runComposeTest {
            // Arrange
            val notifications = List(size = 3) { index ->
                createNotification(
                    title = "Notification $index",
                    supportingText = "The supporting text",
                )
            }
            val openErrorNotificationsActionTitle = "Open notifications"
            val openErrorNotificationsClicked = mutableStateOf(false)
            mainClock.autoAdvance = false
            setContentWithPreviewAndResources {
                TestSubject(
                    notifications = notifications,
                    onOpenErrorNotificationsClick = { openErrorNotificationsClicked.value = true },
                )
            }

            // Act
            onNodeWithTag(BUTTON_NOTIFICATION_TEST_TAG).performClick()
            // Advance Animation
            mainClock.advanceTimeBy(1000L)
            onNodeWithText(openErrorNotificationsActionTitle).performClick()

            // Assert
            printSemanticTree()
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            val listHost = onNodeWithTag(
                BannerInlineNotificationListHostDefaults.TEST_TAG_BANNER_INLINE_LIST,
                useUnmergedTree = true,
            ).assertIsDisplayed()

            listHost
                .onChildren()
                .assertCountEquals(2)

            listHost.assertBannerInline(
                index = 0,
                title = notifications.first().title,
                supportingText = requireNotNull(notifications.first().contentText),
                assertActions = { assertCountEquals(2) },
            )

            listHost.assertBannerInline(
                index = 1,
                title = "Check Error Notifications",
                supportingText = "Some messages need your attention.",
                assertActions = {
                    assertCountEquals(1)
                    val actionButton = filterToOne(
                        matcher = SemanticsMatcher.expectValue(
                            key = SemanticsProperties.Role,
                            expectedValue = Role.Button,
                        ) and hasClickAction(),
                    ).assertIsDisplayed()

                    actionButton
                        .onChildren()
                        .filterToOne(hasTextExactly(openErrorNotificationsActionTitle))
                        .assertIsDisplayed()
                },
            )
        }

    private fun printSemanticTree(root: SemanticsNodeInteraction = composeTestRule.onRoot(useUnmergedTree = true)) {
        println("-----")
        println("Semantic tree:")
        println(root.printToString())
        println("-----")
        println()
    }

    private fun SemanticsNodeInteraction.assertBannerInline(
        index: Int,
        title: String,
        supportingText: String,
        assertActions: SemanticsNodeInteractionCollection.() -> Unit = {},
    ) {
        val banner = onChildAt(index)
        val children = banner.onChildren()
        children
            .filterToOne(hasTextExactly(title))
            .assertIsDisplayed()

        children
            .filterToOne(hasTextExactly(supportingText))
            .assertIsDisplayed()

        children
            .filterToOne(hasTestTag(TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW))
            .onChildren()
            .assertActions()
    }

    @Composable
    private fun TestSubject(
        notifications: List<FakeInAppOnlyNotification>,
        modifier: Modifier = Modifier,
        onActionClick: (NotificationAction) -> Unit = {},
        onOpenErrorNotificationsClick: () -> Unit = {},
    ) {
        Column(modifier = modifier) {
            val state = rememberInAppNotificationHostStateHolder()
            ButtonText(
                text = "Trigger Notification",
                onClick = {
                    notifications.forEach { state.showInAppNotification(it) }
                },
                modifier = Modifier.testTagAsResourceId(BUTTON_NOTIFICATION_TEST_TAG),
            )
            BannerInlineNotificationListHost(
                hostStateHolder = state,
                onActionClick = onActionClick,
                onOpenErrorNotificationsClick = onOpenErrorNotificationsClick,
            )
        }
    }

    private fun createNotification(
        title: String,
        supportingText: String,
        actions: Set<NotificationAction> = setOf(
            createFakeNotificationAction(title = "Action 1"),
            createFakeNotificationAction(title = "Action 2"),
        ),
    ): FakeInAppOnlyNotification {
        return FakeInAppOnlyNotification(
            title = title,
            contentText = supportingText,
            actions = actions,
            inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
        )
    }
}

private fun ComposeTest.setContentWithPreviewAndResources(content: @Composable () -> Unit) = setContentWithTheme {
    // https://github.com/robolectric/robolectric/issues/9603
    // https://youtrack.jetbrains.com/issue/CMP-6612/Support-non-compose-UI-tests-with-resources
    CompositionLocalProvider(LocalInspectionMode provides true) {
        PreviewContextConfigurationEffect()
        content()
    }
}
