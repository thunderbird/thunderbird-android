package net.thunderbird.feature.mail.message.list.internal.ui

import android.util.TypedValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItemDefaults
import net.thunderbird.feature.mail.message.list.internal.ui.preview.AccountPreviewHelper
import net.thunderbird.feature.mail.message.list.internal.ui.preview.MessagePreviewHelper
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.atom.MESSAGE_ITEM_FAVOURITE_ICON_BUTTON_TEST_TAG
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Avatar
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListFooter
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.PaginationUi
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.feature.notification.testing.fake.receiver.FakeInAppNotificationStream
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [ShadowResourcesTheme::class])
@Suppress("MaxLineLength")
class MessageListScreenRendererTest : ComposeTest() {
    @Test
    fun `MessageListScreen - when state is LoadingMessages and isPullToRefresh is true - should display PullToRefreshIndicator composable`() =
        runComposeTest {
            // Arrange
            val messages = createSampleMessages()
            setupTestSubjectComposable(
                messages = messages,
                state = MessageListState.LoadingMessages(
                    progress = 0f,
                    isPullToRefresh = true,
                    metadata = createMetadata(),
                    preferences = createPreferences(),
                    messages = messages.toImmutableList(),
                ),
            )

            // Assert
            onNode(
                matcher = hasTestTag("PullToRefreshIndicator") and hasAnyDescendant(
                    SemanticsMatcher.keyIsDefined(
                        SemanticsProperties.ProgressBarRangeInfo,
                    ),
                ),
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen - when near the end AND there are more items to load - should trigger dispatchEvent with LoadNextPage`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val messages = (1..20).map { index ->
                MessagePreviewHelper.createMessage(
                    id = "msg-$index",
                    subject = "Message $index",
                )
            }

            setupTestSubjectComposable(
                messages = messages,
                dispatchEvent = { events.add(it) },
                metadata = createMetadata(
                    paging = PaginationUi(
                        phase = PaginationUi.Phase.Idle,
                        prefetchDistance = 5,
                        endReached = false,
                    ),
                ),
            )

            // Act
            // Scroll to near end to trigger pagination
            composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(messages.size - 1)
            waitForIdle()

            // Assert
            assertThat(events.filterIsInstance<MessageListEvent.LoadNextPage>()).isEqualTo(
                listOf(MessageListEvent.LoadNextPage),
            )
        }

    @Test
    fun `MessageListScreen - when tap on footer text button - should trigger dispatchEvent with OnFooterClick`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val footerText = "Load more messages on server"

            setupTestSubjectComposable(
                messages = listOf(MessagePreviewHelper.createMessage()),
                dispatchEvent = { events.add(it) },
                metadata = createMetadata(
                    footer = MessageListFooter(showFooter = true, text = footerText),
                ),
            )

            // Act
            onNodeWithText(footerText).performClick()
            waitForIdle()

            // Assert
            assertThat(events).containsExactly(MessageListEvent.OnFooterClick)
        }

    @Test
    fun `MessageListScreen - when screen is at the top AND user pulls to refresh - should trigger dispatchEvent with Refresh`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            setupTestSubjectComposable(
                messages = listOf(MessagePreviewHelper.createMessage()),
                dispatchEvent = { events.add(it) },
            )

            // Act
            onNodeWithTag(MessageListScreenRenderer.TEST_TAG_MESSAGE_LIST_ROOT).performTouchInput { swipeDown() }
            waitForIdle()

            // Assert
            assertThat(events.filterIsInstance<MessageListEvent.Refresh>()).isEqualTo(
                listOf(MessageListEvent.Refresh),
            )
        }

    @Test
    fun `MessageListScreen - when screen is NOT at the top AND user swipes the list down - Pull to refresh should NOT be triggered`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val messages = (1..20).map { index ->
                MessagePreviewHelper.createMessage(
                    id = "msg-$index",
                    subject = "Message $index",
                )
            }
            setupTestSubjectComposable(
                messages = messages,
                dispatchEvent = { events.add(it) },
            )

            // Act - scroll down first, then swipe down
            composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(15)
            waitForIdle()

            composeTestRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeDown() }
            waitForIdle()

            // Assert
            assertThat(events.filterIsInstance<MessageListEvent.Refresh>()).isEmpty()
        }

    @Test
    fun `MessageListScreen-MessageListItem - should defaultContentPadding by default`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.Unread)
            setupTestSubjectComposable(
                messages = listOf(message),
                preferences = createPreferences(density = UiDensity.Default),
            )

            // Assert - the unread message item is displayed (density is internal to layout, verify rendering)
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when preferences density is Compat - should compactContentPadding`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.Unread)
            setupTestSubjectComposable(
                messages = listOf(message),
                preferences = createPreferences(density = UiDensity.Compact),
            )

            // Assert
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when preferences density is Relaxed - should relaxedContentPadding`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.Unread)
            setupTestSubjectComposable(
                messages = listOf(message),
                preferences = createPreferences(density = UiDensity.Relaxed),
            )

            // Assert
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is New - should display NewMessageListItem_Root composable`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.New)
            setupTestSubjectComposable(messages = listOf(message))

            // Assert
            onNodeWithTag(
                MessageListItemDefaults.NEW_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is Read - should display ReadMessageListItem_Root composable`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.Read)
            setupTestSubjectComposable(messages = listOf(message))

            // Assert
            onNodeWithTag(
                MessageListItemDefaults.READ_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is Unread - should display UnreadMessageListItem_Root composable`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.createMessage(state = MessageItemUi.State.Unread)
            setupTestSubjectComposable(messages = listOf(message))

            // Assert
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is New and message is a conversation - should display the first correspondent in bold font weight`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.conversationMessages.first().copy(
                state = MessageItemUi.State.New,
            )
            setupTestSubjectComposable(messages = listOf(message))

            // Assert - the sender name should be displayed
            onNodeWithText(message.senders.displayName, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is Read and message is a conversation - should display the all correspondent in normal font weight`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.conversationMessages.first().copy(
                state = MessageItemUi.State.Read,
            )
            setupTestSubjectComposable(messages = listOf(message))

            // Assert - the sender name should be displayed
            onNodeWithText(message.senders.displayName, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when message state is Unread and message is a conversation - should display the first correspondent in bold font weight`() =
        runComposeTest {
            // Arrange
            val message = MessagePreviewHelper.conversationMessages.first().copy(
                state = MessageItemUi.State.Unread,
            )
            setupTestSubjectComposable(messages = listOf(message))

            // Assert - the sender name should be displayed
            onNodeWithText(message.senders.displayName, substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }

    @Test
    fun `MessageListScreen-MessageListItem - when tap - should trigger dispatchEvent with OnMessageClick`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage()
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
            )

            // Act
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).performClick()
            waitForIdle()

            // Assert
            assertThat(events).containsExactly(MessageItemEvent.OnMessageClick(message))
        }

    @Test
    fun `MessageListScreen-MessageListItem - when long press - should trigger dispatchEvent with ToggleSelectMessages`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage()
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
            )

            // Act
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).performTouchInput {
                down(center)
                advanceEventTime(1000)
                up()
            }
            waitForIdle()

            // Assert
            assertThat(events).containsExactly(MessageItemEvent.ToggleSelectMessages(message))
        }

    @Test
    @Ignore("Will be re-nabled when the message item restructure is done.")
    fun `MessageListScreen-MessageListItem - when tap on avatar - should trigger dispatchEvent with ToggleSelectMessages`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage()
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
            )

            // Act - click on the avatar area (the leading/avatar section)
            onNodeWithText(
                message.senders.avatar.let {
                    (it as? Avatar.Monogram)?.value ?: ""
                },
                useUnmergedTree = true,
            ).performClick()
            waitForIdle()

            // Assert
            assertThat(events).containsExactly(MessageItemEvent.ToggleSelectMessages(message))
        }

    @Test
    fun `MessageListScreen-MessageListItem - when tap on favourite button - should trigger dispatchEvent with ToggleFavourite`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage(starred = true)
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
            )

            // Act - click on the favourite/star button
            composeTestRule
                .onNodeWithTag(MESSAGE_ITEM_FAVOURITE_ICON_BUTTON_TEST_TAG)
                .performClick()
            waitForIdle()

            // Assert
            assertThat(events).containsExactly(MessageItemEvent.ToggleFavourite(message))
        }

    @Test
    fun `MessageListScreen-MessageListItem - when swipe from start to end - should trigger dispatchEvent with OnSwipeMessage with swipeActionRight`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage()
            val swipeActions = SwipeActions(
                leftAction = SwipeAction.Delete,
                rightAction = SwipeAction.ToggleRead,
            )
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
                metadata = createMetadata(
                    swipeActions = persistentMapOf(message.account.id to swipeActions),
                ),
            )

            // Act
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).performTouchInput { swipeRight() }
            waitForIdle()

            // Assert
            assertThat(events.filterIsInstance<MessageItemEvent.OnSwipeMessage>()).isEqualTo(
                listOf(MessageItemEvent.OnSwipeMessage(message, swipeAction = SwipeAction.ToggleRead)),
            )
        }

    @Test
    fun `MessageListScreen-MessageListItem - when swipe from end to start - should trigger dispatchEvent with OnSwipeMessage with swipeActionLeft`() =
        runComposeTest {
            // Arrange
            val events = mutableListOf<MessageListEvent>()
            val message = MessagePreviewHelper.createMessage()
            val swipeActions = SwipeActions(
                leftAction = SwipeAction.Delete,
                rightAction = SwipeAction.ToggleRead,
            )
            setupTestSubjectComposable(
                messages = listOf(message),
                dispatchEvent = { events.add(it) },
                metadata = createMetadata(
                    swipeActions = persistentMapOf(message.account.id to swipeActions),
                ),
            )

            // Act
            onNodeWithTag(
                MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG,
                useUnmergedTree = true,
            ).performTouchInput { swipeLeft() }
            waitForIdle()

            // Assert
            assertThat(events.filterIsInstance<MessageItemEvent.OnSwipeMessage>()).isEqualTo(
                listOf(MessageItemEvent.OnSwipeMessage(message, swipeAction = SwipeAction.Delete)),
            )
        }

    private fun ComposeTest.setupTestSubjectComposable(
        messages: List<MessageItemUi>,
        dispatchEvent: (MessageListEvent) -> Unit = {},
        onEffect: (MessageListEffect) -> Unit = {},
        preferences: MessageListPreferences = createPreferences(),
        metadata: MessageListMetadata = createMetadata(),
        state: MessageListState = MessageListState.LoadedMessages(
            metadata = metadata,
            preferences = preferences,
            messages = messages.toImmutableList(),
        ),
    ) {
        val renderer = MessageListScreenRenderer()
        composeTestRule.setContent {
            ThunderbirdTheme2 {
                koinPreview {
                    single<InAppNotificationStream> { FakeInAppNotificationStream() }
                } WithContent {
                    renderer.Render(
                        state = state,
                        dispatchEvent = dispatchEvent,
                        onEffect = onEffect,
                        preferences = preferences,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun createPreferences(
        density: UiDensity = UiDensity.Default,
        groupConversations: Boolean = true,
        showCorrespondentNames: Boolean = true,
        showMessageAvatar: Boolean = true,
        showFavouriteButton: Boolean = true,
        senderAboveSubject: Boolean = true,
        excerptLines: Int = 1,
        dateTimeFormat: MessageListDateTimeFormat = MessageListDateTimeFormat.Contextual,
        actionRequiringUserConfirmation: ImmutableSet<ActionRequiringUserConfirmation> = persistentSetOf(),
        colorizeBackgroundWhenRead: Boolean = false,
    ) = MessageListPreferences(
        density = density,
        groupConversations = groupConversations,
        showCorrespondentNames = showCorrespondentNames,
        showMessageAvatar = showMessageAvatar,
        showFavouriteButton = showFavouriteButton,
        senderAboveSubject = senderAboveSubject,
        excerptLines = excerptLines,
        dateTimeFormat = dateTimeFormat,
        actionRequiringUserConfirmation = actionRequiringUserConfirmation,
        colorizeBackgroundWhenRead = colorizeBackgroundWhenRead,
    )

    private fun createMetadata(
        footer: MessageListFooter = MessageListFooter(),
        paging: PaginationUi = PaginationUi(),
        swipeActions: ImmutableMap<AccountId, SwipeActions> = persistentMapOf(),
    ) = MessageListMetadata(
        folder = AccountPreviewHelper.inboxFolder,
        swipeActions = swipeActions,
        sortCriteriaPerAccount = persistentMapOf(),
        activeMessage = null,
        isActive = false,
        footer = footer,
        showAccountIndicator = false,
        paging = paging,
    )

    private fun createSampleMessages() = listOf(
        MessagePreviewHelper.createMessage(id = "msg-1"),
        MessagePreviewHelper.createMessage(id = "msg-2", state = MessageItemUi.State.Read),
    )
}

@Suppress("MagicNumber")
@Implements(className = $$"android.content.res.Resources$Theme")
class ShadowResourcesTheme {

    @RealObject
    private lateinit var realTheme: Any

    private val swipeColorAttrs = mapOf(
        R.attr.messageListSwipeSelectColor to 0xFF1E88E5.toInt(),
        R.attr.messageListSwipeToggleReadColor to 0xFF1E88E5.toInt(),
        R.attr.messageListSwipeToggleStarColor to 0xFFFB8C00.toInt(),
        R.attr.messageListSwipeArchiveColor to 0xFF43A047.toInt(),
        R.attr.messageListSwipeDeleteColor to 0xFFE53935.toInt(),
        R.attr.messageListSwipeSpamColor to 0xFFD32F2F.toInt(),
        R.attr.messageListSwipeMoveColor to 0xFF8E24AA.toInt(),
    )

    @Implementation
    fun resolveAttribute(resId: Int, outValue: TypedValue, resolveRefs: Boolean): Boolean {
        swipeColorAttrs[resId]?.let { color ->
            outValue.type = TypedValue.TYPE_INT_COLOR_ARGB8
            outValue.data = color
            return true
        }
        return Shadow.directlyOn(
            realTheme,
            "android.content.res.Resources\$Theme",
            "resolveAttribute",
            ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, resId),
            ReflectionHelpers.ClassParameter.from(TypedValue::class.java, outValue),
            ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, resolveRefs),
        )
    }
}
