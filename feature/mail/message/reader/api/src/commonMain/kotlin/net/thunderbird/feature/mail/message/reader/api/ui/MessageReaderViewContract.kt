package net.thunderbird.feature.mail.message.reader.api.ui

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.message.reader.api.domain.MessageReaderAction
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

interface MessageReaderViewContract {
    abstract class ViewModel<TPart>(state: State<TPart>) :
        BaseViewModel<State<TPart>, Event<TPart>, Effect>(initialState = state)

    data class State<TPart>(
        val showReaderActionsBottomSheet: Boolean = false,
        val messageReaderActions: ImmutableSet<MessageReaderAction> =
            persistentSetOf(MessageReaderAction.Reply, MessageReaderAction.ReplyAll),
        val attachments: ImmutableList<AttachmentUiItem<TPart>> = persistentListOf(),
    )

    sealed interface Event<TPart> {
        data class UpdateAttachments<TPart>(
            val nonInlineAttachments: List<AttachmentViewInfoMapper.AttachmentMetadata<TPart>>,
            val extraNonInlineAttachments: List<AttachmentViewInfoMapper.AttachmentMetadata<TPart>>,
        ) : Event<TPart>

        class OpenMessageReaderBottomSheet<TPart> : Event<TPart>
        class CloseMessageReaderBottomSheet<TPart> : Event<TPart>

        data class OnMessageReaderBottomSheetActionClick<TPart>(val action: MessageReaderAction) : Event<TPart>
    }

    sealed interface Effect {
        data object TriggerOnReplyListener : Effect
        data object TriggerOnReplyAllListener : Effect
    }
}
