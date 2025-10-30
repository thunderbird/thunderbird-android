package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.core.ui.compose.theme2.MainTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.style.NotificationPriority
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyle
import net.thunderbird.feature.notification.api.ui.util.assertBannerInline
import net.thunderbird.feature.notification.api.ui.util.assertBannerInlineList
import net.thunderbird.feature.notification.api.ui.util.printSemanticTree
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.ui.action.createFakeNotificationAction
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

@Suppress("MaxLineLength")
class InAppNotificationScaffoldTest : ComposeTest() {
    // region [ content lambda with scroll verification ]
    @Test
    fun `InAppNotificationScaffold should be displayed`() = runComposeTestSuspend {
        // Arrange & Act
        setTestSubjectContent {
            InAppNotificationScaffold {
                TextBodySmall("Scaffold")
            }
        }

        // Assert
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_INNER_SCAFFOLD)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_IN_APP_NOTIFICATION_HOST)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST)
            .assertIsNotDisplayed()
    }

    @Test
    fun `InAppNotificationScaffold should support Column with verticalScroll modifier`() = runComposeTestSuspend {
        // Arrange
        val contentTag = "content_tag"
        val firstElementTag = "first_element_tag"
        val lastElementTag = "last_element_tag"
        val size = 200
        setTestSubjectContent {
            InAppNotificationScaffold { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .testTag(contentTag),
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                ) {
                    repeat(size) { index ->
                        TextBodyLarge(
                            text = "Element $index",
                            modifier = when (index) {
                                0 -> Modifier.testTag(firstElementTag)
                                size - 1 -> Modifier.testTag(lastElementTag)
                                else -> Modifier
                            },
                        )
                    }
                }
            }
        }

        // Act
        onNodeWithTag(contentTag).performScrollToNode(hasTestTag(lastElementTag))

        // Assert
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_INNER_SCAFFOLD)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_IN_APP_NOTIFICATION_HOST)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST)
            .assertIsNotDisplayed()

        printSemanticTree()
        onNodeWithTag(firstElementTag).assertIsNotDisplayed()
        onNodeWithTag(lastElementTag).assertIsDisplayed()
    }

    @Test
    fun `InAppNotificationScaffold should support LazyColumn`() = runComposeTestSuspend {
        // Arrange
        val contentTag = "content_tag"
        val firstElementTag = "first_element_tag"
        val lastElementTag = "last_element_tag"
        val size = 200
        setTestSubjectContent {
            InAppNotificationScaffold { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .testTag(contentTag),
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                ) {
                    items(size) { index ->
                        TextBodyLarge(
                            text = "Element $index",
                            modifier = when (index) {
                                0 -> Modifier.testTag(firstElementTag)
                                size - 1 -> Modifier.testTag(lastElementTag)
                                else -> Modifier
                            },
                        )
                    }
                }
            }
        }

        // Act
        onNodeWithTag(contentTag).performScrollToNode(hasTestTag(lastElementTag))

        // Assert
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_INNER_SCAFFOLD)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_IN_APP_NOTIFICATION_HOST)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST)
            .assertIsNotDisplayed()

        printSemanticTree()
        onNodeWithTag(firstElementTag).assertIsNotDisplayed()
        onNodeWithTag(lastElementTag).assertIsDisplayed()
    }
    // endregion [ content lambda with scroll verification ]

    // region [ Banner Global Notification verification ]
    @Test
    fun `InAppNotificationScaffold should display BannerGlobalHost when Show event with bannerGlobal in-app notification is triggered`() =
        runComposeTestSuspend {
            // Arrange
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerGlobal() },
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
                .assertIsDisplayed()
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()
        }

    @Test
    fun `InAppNotificationScaffold should call onNotificationActionClick when banner global action is clicked`() =
        runComposeTestSuspend {
            // Arrange
            val actionTitle = "The action"
            val action = createFakeNotificationAction(actionTitle)
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerGlobal() },
                    actions = setOf(action),
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            val clickedAction = mutableStateOf<NotificationAction?>(value = null)
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    onNotificationActionClick = { clickedAction.value = it },
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act (Phase 1)
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert (Phase 1)
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
                .assertIsDisplayed()
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()

            // Act (Phase 2)
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_BANNER_GLOBAL_ACTION)
                .performClick()

            // Assert (Phase 2)
            assertThat(clickedAction.value)
                .isNotNull()
                .given { action ->
                    assertThat(action.resolveTitle())
                        .isEqualTo(actionTitle)
                }
        }

    @Test
    fun `InAppNotificationScaffold should display the most priority banner global notification when multiple banner global notifications are triggered`() =
        runComposeTestSuspend {
            // Arrange
            val lowerPriorityNotificationText = "lower priority notification"
            val lowerPriorityNotification = FakeInAppOnlyNotification(
                contentText = lowerPriorityNotificationText,
                severity = NotificationSeverity.Warning,
                inAppNotificationStyle = inAppNotificationStyle { bannerGlobal(priority = NotificationPriority.Min) },
            )
            val higherPriorityNotificationText = "higher priority notification"
            val higherPriorityNotification = FakeInAppOnlyNotification(
                contentText = higherPriorityNotificationText,
                severity = NotificationSeverity.Warning,
                inAppNotificationStyle = inAppNotificationStyle { bannerGlobal(priority = NotificationPriority.Max) },
            )

            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act (Phase 1)
            printSemanticTree()
            receiver.triggerEvent(InAppNotificationEvent.Show(notification = lowerPriorityNotification))
            printSemanticTree()

            // Assert (Phase 1)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(lowerPriorityNotificationText)

            // Act (Phase 2)
            printSemanticTree()
            receiver.triggerEvent(InAppNotificationEvent.Show(notification = higherPriorityNotification))
            printSemanticTree()

            // Assert (Phase 2)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(higherPriorityNotificationText)
        }

    @Test
    fun `InAppNotificationScaffold should display the previous banner global notification when higher priority banner global notification is dismissed`() =
        runComposeTestSuspend {
            // Arrange
            val lowerPriorityNotificationText = "lower priority notification"
            val lowerPriorityNotification = FakeInAppOnlyNotification(
                contentText = lowerPriorityNotificationText,
                severity = NotificationSeverity.Warning,
                inAppNotificationStyle = inAppNotificationStyle { bannerGlobal(priority = NotificationPriority.Min) },
            )
            val higherPriorityNotificationText = "higher priority notification"
            val higherPriorityNotification = FakeInAppOnlyNotification(
                contentText = higherPriorityNotificationText,
                severity = NotificationSeverity.Warning,
                inAppNotificationStyle = inAppNotificationStyle { bannerGlobal(priority = NotificationPriority.Max) },
            )

            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act (Phase 1)
            printSemanticTree(prefixLabel = "before first show event")
            receiver.triggerEvent(InAppNotificationEvent.Show(notification = lowerPriorityNotification))
            printSemanticTree(prefixLabel = "before second show event")
            receiver.triggerEvent(InAppNotificationEvent.Show(notification = higherPriorityNotification))
            printSemanticTree(prefixLabel = "after events triggered")

            // Assert (Phase 1)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(higherPriorityNotificationText)

            // Act (Phase 2)
            printSemanticTree(prefixLabel = "before dismiss event")
            receiver.triggerEvent(InAppNotificationEvent.Dismiss(notification = higherPriorityNotification))
            printSemanticTree(prefixLabel = "after dismiss event triggered")

            // Assert (Phase 2)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsNotDisplayed()
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_WARNING_BANNER)
                .assertIsDisplayed()
                .onChild()
                .assertTextEquals(lowerPriorityNotificationText)
        }

    @Test
    fun `InAppNotificationScaffold should not display BannerGlobalHost when Show event with bannerInline in-app notification is triggered`() =
        runComposeTestSuspend {
            // Arrange
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                    actions = setOf(createFakeNotificationAction("Action")),
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
                .assertIsNotDisplayed()
        }
    // endregion [ Banner Global Notification verification ]

    // region [ Banner Inline List Notification verification ]
    @Test
    fun `InAppNotificationScaffold should display BannerInlineListHost when Show event with bannerInline in-app notification is triggered`() =
        runComposeTestSuspend {
            // Arrange
            val notification = FakeInAppOnlyNotification(
                title = "The notification",
                contentText = "The content",
                inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                actions = setOf(createFakeNotificationAction("Action")),
            )
            val event = InAppNotificationEvent.Show(notification = notification)
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            assertBannerInlineList(size = 1) {
                assertIsDisplayed()
                assertBannerInline(
                    index = 0,
                    title = notification.title,
                    supportingText = requireNotNull(notification.contentText),
                )
            }
        }

    @Test
    fun `InAppNotificationScaffold should display BannerInlineListHost with check error notifications when more than 3 Show event with bannerInline in-app notification is triggered`() =
        runComposeTestSuspend {
            mainClock.autoAdvance = false
            // Arrange
            val notification = FakeInAppOnlyNotification(
                title = "The notification",
                contentText = "The content",
                inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                actions = setOf(createFakeNotificationAction("Action")),
            )
            val event = InAppNotificationEvent.Show(notification = notification)
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold { TextBodyLarge(text = "Content") }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            mainClock.advanceTimeBy(milliseconds = 1000L)
            repeat(times = 10) {
                receiver.triggerEvent(
                    InAppNotificationEvent.Show(
                        notification = FakeInAppOnlyNotification(
                            title = "Notification $it",
                            inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                            actions = setOf(createFakeNotificationAction("Action")),
                        ),
                    ),
                )
                mainClock.advanceTimeBy(1000L)
            }
            printSemanticTree()

            // Assert
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            assertBannerInlineList(size = 2) {
                assertIsDisplayed()
                assertBannerInline(
                    index = 0,
                    title = notification.title,
                    supportingText = requireNotNull(notification.contentText),
                )
                assertBannerInline(
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
        }

    @Test
    fun `InAppNotificationScaffold should call onNotificationActionClick when banner inline list action is clicked`() =
        runComposeTestSuspend {
            // Arrange
            val actionTitle = "The action"
            val action = createFakeNotificationAction(actionTitle)
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                    actions = setOf(action),
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            val clickedAction = mutableStateOf<NotificationAction?>(value = null)
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    onNotificationActionClick = { clickedAction.value = it },
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act (Phase 1)
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert (Phase 1)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            // Act (Phase 2)
            onNodeWithTag(
                BannerInlineNotificationListHostDefaults.testTagBannerInlineListItemAction(
                    index = 0,
                    actionIndex = 0,
                ),
            ).performClick()

            // Assert (Phase 2)
            assertThat(clickedAction.value)
                .isNotNull()
                .given { action ->
                    assertThat(action.resolveTitle())
                        .isEqualTo(actionTitle)
                }
        }

    @Test
    fun `InAppNotificationScaffold should call onNotificationActionClick when check notifications error action is clicked`() =
        runComposeTestSuspend {
            // Arrange
            mainClock.autoAdvance = false
            val receiver = FakeInAppNotificationReceiver()
            val clickedAction = mutableStateOf<NotificationAction?>(value = null)
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    onNotificationActionClick = { clickedAction.value = it },
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act (Phase 1)
            printSemanticTree()
            repeat(times = 10) {
                receiver.triggerEvent(
                    InAppNotificationEvent.Show(
                        notification = FakeInAppOnlyNotification(
                            title = "Notification $it",
                            inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                            actions = setOf(createFakeNotificationAction("Action")),
                        ),
                    ),
                )
                mainClock.advanceTimeBy(1000L)
            }
            printSemanticTree()

            // Assert (Phase 1)
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertIsDisplayed()

            assertBannerInlineList(size = 2)

            // Act (Phase 2)
            mainClock.autoAdvance = true
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_CHECK_ERROR_NOTIFICATIONS_ACTION)
                .performClick()

            // Assert (Phase 2)
            assertThat(clickedAction.value)
                .isNull()

            onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_ERROR_NOTIFICATIONS_DIALOG)
                .assertIsDisplayed()
        }
    // endregion [ Banner Inline List Notification verification ]

    // region [ DisplayInAppNotificationFlag verification ]
    @Test
    fun `InAppNotificationScaffold should not display BannerGlobalHost when display flag BannerGlobalNotifications is not enabled`() =
        runComposeTestSuspend {
            // Arrange
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerGlobal() },
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    enabled = persistentSetOf(), // Empty set will disable all display flags
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Pre-Act Assert
            assertIdleState()

            // Assert
            onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
                .assertExists()
                .assertIsNotDisplayed()
        }

    @Test
    fun `InAppNotificationScaffold should not display BannerInlineListHost when display flag BannerInlineNotifications is not enabled`() =
        runComposeTestSuspend {
            // Arrange
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { bannerInline() },
                    actions = setOf(createFakeNotificationAction("Action")),
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    enabled = persistentSetOf(), // Empty set will disable all display flags
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert
            onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
                .assertExists()
                .assertIsNotDisplayed()
        }

    @Test
    fun `InAppNotificationScaffold should not display Snackbar when display flag SnackbarNotifications is not enabled`() =
        runComposeTestSuspend {
            // Arrange
            val event = InAppNotificationEvent.Show(
                notification = FakeInAppOnlyNotification(
                    inAppNotificationStyle = inAppNotificationStyle { snackbar() },
                    actions = setOf(createFakeNotificationAction("Action")),
                ),
            )
            val receiver = FakeInAppNotificationReceiver()
            setTestSubjectContent(inAppNotificationReceiver = receiver) {
                InAppNotificationScaffold(
                    enabled = persistentSetOf(), // Empty set will disable all display flags
                ) {
                    TextBodyLarge(text = "Content")
                }
            }

            // Pre-Act Assert
            assertIdleState()

            // Act
            printSemanticTree()
            receiver.triggerEvent(event)
            printSemanticTree()

            // Assert
            onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST)
                .assertExists()
                .assertIsNotDisplayed()
        }
    // endregion [ DisplayInAppNotificationFlag verification ]

    private fun ComposeContentTestRule.assertIdleState() {
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_INNER_SCAFFOLD)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_IN_APP_NOTIFICATION_HOST)
            .assertExists()
        onNodeWithTag(InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST)
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag(BannerGlobalNotificationHostDefaults.TEST_TAG_HOST)
            .assertExists()
            .assertIsNotDisplayed()
        onNodeWithTag(BannerInlineNotificationListHostDefaults.TEST_TAG_HOST_PARENT)
            .assertExists()
            .assertIsNotDisplayed()
    }

    private fun ComposeTest.setTestSubjectContent(
        inAppNotificationReceiver: InAppNotificationReceiver = FakeInAppNotificationReceiver(),
        content: @Composable () -> Unit,
    ) {
        setContentWithTheme {
            koinPreview {
                single<InAppNotificationReceiver> { inAppNotificationReceiver }
            } WithContent {
                // https://github.com/robolectric/robolectric/issues/9603
                // https://youtrack.jetbrains.com/issue/CMP-6612/Support-non-compose-UI-tests-with-resources
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    PreviewContextConfigurationEffect()
                    content()
                }
            }
        }
    }
}

private class FakeInAppNotificationReceiver(
    initialEvents: List<InAppNotificationEvent> = emptyList(),
) : InAppNotificationReceiver {
    private val _events = MutableSharedFlow<InAppNotificationEvent>(replay = 1)
    override val events: SharedFlow<InAppNotificationEvent> = _events

    init {
        initialEvents.forEach { event -> _events.tryEmit(event) }
    }

    suspend fun triggerEvent(event: InAppNotificationEvent) {
        _events.emit(event)
    }
}
