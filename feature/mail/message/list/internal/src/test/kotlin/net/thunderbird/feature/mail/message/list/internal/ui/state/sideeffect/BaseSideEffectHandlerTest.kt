package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

open class BaseSideEffectHandlerTest {

    protected fun createReadyWarmingUpState() = MessageListState.WarmingUp(
        metadata = createReadyMetadata(),
        preferences = createMessageListPreferences(),
    )

    protected fun createLoadedMessagesState(
        activeMessage: MessageItemUi? = null,
    ) = MessageListState.LoadedMessages(
        metadata = createReadyMetadata().copy(activeMessage = activeMessage),
        preferences = createMessageListPreferences(),
        messages = persistentListOf(),
    )

    protected fun createSelectingMessagesState() = MessageListState.SelectingMessages(
        metadata = createReadyMetadata(),
        preferences = createMessageListPreferences(),
        messages = persistentListOf(),
    )

    protected fun createLoadingMessagesState(
        progress: Float = 0f,
        metadata: MessageListMetadata = createMetadata(),
        preferences: MessageListPreferences = createMessageListPreferences(),
    ) = MessageListState.LoadingMessages(
        progress = progress,
        metadata = metadata,
        preferences = preferences,
    )

    protected fun createReadyMetadata() = MessageListMetadata(
        folder = Folder(
            id = "fake",
            account = Account(id = UnifiedAccountId, color = Color.Unspecified),
            name = "Inbox",
            type = FolderType.INBOX,
        ),
        swipeActions = persistentMapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        sortCriteriaPerAccount = persistentMapOf(null to SortCriteria(primary = SortType.DateDesc)),
        activeMessage = null,
        isActive = false,
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

    protected fun createMetadata() = MessageListMetadata(
        folder = Folder(
            id = "folderId",
            account = Account(id = AccountIdFactory.create(), color = Color.Unspecified),
            name = "Inbox",
            type = FolderType.INBOX,
        ),
        swipeActions = persistentMapOf(),
        sortCriteriaPerAccount = persistentMapOf(),
        activeMessage = null,
        isActive = true,
    )

    fun createMessageItemUi(
        id: String = "1",
    ) = MessageItemUi(
        state = MessageItemUi.State.Unread,
        id = id,
        messageReference = "ref-$id",
        account = Account(id = AccountIdFactory.create(), color = Color.Unspecified),
        senders = ComposedAddressUi(displayName = "sender"),
        subject = "subject",
        excerpt = "excerpt",
        formattedReceivedAt = "Jan 2026",
        hasAttachments = false,
        starred = false,
        encrypted = false,
        answered = false,
        forwarded = false,
        selected = false,
    )
}
