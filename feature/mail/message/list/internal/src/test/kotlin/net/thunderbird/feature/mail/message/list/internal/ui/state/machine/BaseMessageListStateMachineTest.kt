@file:OptIn(ExperimentalCoroutinesApi::class)

package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.debugging.DebuggingSettings
import net.thunderbird.core.preference.debugging.DebuggingSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.testing.TestClock
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.FolderEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListSearchEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi.State

open class BaseMessageListStateMachineTest {
    protected fun TestScope.createStateMachine(
        dispatch: (MessageListEvent) -> Unit = {},
        dispatchUiEffect: (MessageListEffect) -> Unit = {},
    ) = MessageListStateMachine(
        logger = TestLogger(),
        clock = TestClock(),
        scope = this,
        dispatch = dispatch,
        dispatchUiEffect = dispatchUiEffect,
        debuggingSettingsPreferenceManager = FakeDebuggingSettingsPreferenceManager(),
    )

    protected suspend fun TestScope.createStateMachineOnLoadingState(
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortCriteriaPerAccount: Map<AccountId?, SortCriteria> = mapOf(null to SortCriteria(SortType.DateDesc)),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        folder: Folder = createFolder(),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = FolderEvent.FolderLoaded(folder = folder))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        advanceUntilIdle()
        return stateMachine
    }

    protected suspend fun TestScope.createStateMachineOnLoadedState(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortCriteriaPerAccount: Map<AccountId?, SortCriteria> = mapOf(null to SortCriteria(SortType.DateDesc)),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        folder: Folder = createFolder(),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = FolderEvent.FolderLoaded(folder = folder))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        advanceUntilIdle()
        return stateMachine
    }

    protected suspend fun TestScope.createStateMachineOnSearchingMessages(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortCriteriaPerAccount: Map<AccountId?, SortCriteria> = mapOf(null to SortCriteria(SortType.DateDesc)),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        folder: Folder = createFolder(),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        stateMachine.process(MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = FolderEvent.FolderLoaded(folder = folder))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        stateMachine.process(event = MessageListSearchEvent.EnterSearchMode)
        advanceUntilIdle()
        return stateMachine
    }

    protected suspend fun TestScope.createStateMachineOnSelectingMessages(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortCriteriaPerAccount: Map<AccountId?, SortCriteria> = mapOf(null to SortCriteria(SortType.DateDesc)),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        folder: Folder = createFolder(),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = FolderEvent.FolderLoaded(folder = folder))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        stateMachine.process(event = MessageListEvent.EnterSelectionMode)
        advanceUntilIdle()
        return stateMachine
    }

    protected fun createMessageUiItemList(
        size: Int,
        accountId: AccountId = AccountIdFactory.create(),
        builder: (index: Int) -> MessageItemUi = { index ->
            when {
                index % 6 == 0 -> createMessageUiItem(
                    state = State.Unread,
                    id = "id$index",
                    accountId = accountId,
                )

                index % 4 == 0 -> createMessageUiItem(
                    state = State.Read,
                    id = "id$index",
                    accountId = accountId,
                )

                index % 2 == 0 -> createMessageUiItem(
                    state = State.New,
                    id = "id$index",
                    accountId = accountId,
                )

                else -> createMessageUiItem(
                    state = State.Unread,
                    id = "id$index",
                    accountId = accountId,
                ).copy(active = true)
            }
        },
    ): List<MessageItemUi> = List(size) { builder(it) }

    protected fun createMessageUiItem(
        state: State,
        id: String,
        messageReference: String = "message_reference",
        accountId: AccountId = AccountIdFactory.create(),
        senders: ComposedAddressUi = ComposedAddressUi(displayName = "sender"),
        subject: String = "mock subject",
        excerpt: String = "mock excerpt",
        formattedReceivedAt: String = "Jan 2026",
        hasAttachments: Boolean = false,
        starred: Boolean = false,
        encrypted: Boolean = false,
        answered: Boolean = false,
        forwarded: Boolean = false,
        selected: Boolean = false,
        threadCount: Int = 0,
    ): MessageItemUi = MessageItemUi(
        state = state,
        id = id,
        messageReference = messageReference,
        account = Account(id = accountId, color = Color.Unspecified),
        senders = senders,
        subject = subject,
        excerpt = excerpt,
        formattedReceivedAt = formattedReceivedAt,
        hasAttachments = hasAttachments,
        starred = starred,
        encrypted = encrypted,
        answered = answered,
        forwarded = forwarded,
        selected = selected,
        threadCount = threadCount,
    )

    protected fun createFolder(
        id: String = "fake",
        account: Account = Account(id = UnifiedAccountId, Color.Unspecified),
        name: String = "unified",
        type: FolderType = FolderType.INBOX,
        parent: Folder? = null,
        root: Folder? = null,
        canExpunge: Boolean = false,
    ): Folder = Folder(
        id = id,
        account = account,
        name = name,
        type = type,
        parent = parent,
        root = root,
        canExpunge = canExpunge,
    )

    protected fun createMessageListPreferences(
        density: UiDensity = UiDensity.Default,
        groupConversations: Boolean = false,
        showCorrespondentNames: Boolean = false,
        showMessageAvatar: Boolean = false,
        showFavouriteButton: Boolean = false,
        senderAboveSubject: Boolean = false,
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

    protected class FakeDebuggingSettingsPreferenceManager(
        protected val enabledDebug: Boolean = true,
    ) : DebuggingSettingsPreferenceManager {
        override fun save(config: DebuggingSettings) {
            TODO("Not yet implemented")
        }

        override fun getConfig(): DebuggingSettings = DebuggingSettings(
            isDebugLoggingEnabled = enabledDebug,
            isSyncLoggingEnabled = false,
            isSensitiveLoggingEnabled = false,
        )

        override fun getConfigFlow(): Flow<DebuggingSettings> = flowOf(getConfig())
    }
}
