package net.thunderbird.feature.mail.message.reader.impl.ui

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.mail.message.reader.api.domain.MessageReaderAction
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.ui.MessageReaderViewContract

class MessageReaderViewModel(
    private val attachmentViewInfoMapper: AttachmentViewInfoMapper<MailPart>,
    initialState: MessageReaderViewContract.State<MailPart> = MessageReaderViewContract.State(),
) : MessageReaderViewContract.ViewModel<MailPart>(state = initialState) {
    override fun event(event: MessageReaderViewContract.Event<MailPart>) {
        when (event) {
            is MessageReaderViewContract.Event.UpdateAttachments -> updateAttachments(event)

            is MessageReaderViewContract.Event.CloseMessageReaderBottomSheet<*> -> updateState {
                it.copy(showReaderActionsBottomSheet = false)
            }

            is MessageReaderViewContract.Event.OnMessageReaderBottomSheetActionClick<*> ->
                handleOnMessageReaderBottomSheetActionClick(event.action)

            is MessageReaderViewContract.Event.OpenMessageReaderBottomSheet<*> -> updateState {
                it.copy(showReaderActionsBottomSheet = true)
            }
        }
    }

    private fun handleOnMessageReaderBottomSheetActionClick(action: MessageReaderAction) {
        updateState { state ->
            state.copy(showReaderActionsBottomSheet = false)
        }

        when (action) {
            MessageReaderAction.Reply -> emitEffect(MessageReaderViewContract.Effect.TriggerOnReplyListener)
            MessageReaderAction.ReplyAll -> emitEffect(MessageReaderViewContract.Effect.TriggerOnReplyAllListener)
        }
    }

    private fun updateAttachments(event: MessageReaderViewContract.Event.UpdateAttachments<MailPart>) {
        val attachments = event.nonInlineAttachments + event.extraNonInlineAttachments
        updateState { state ->
            state.copy(
                attachments = with(attachmentViewInfoMapper) {
                    attachments
                        .filterNot { it.isInlineAttachment() }
                        .map { it.toUiItem(encrypted = it in event.extraNonInlineAttachments) }
                        .toPersistentList()
                },
            )
        }
    }
}

expect interface MailPart
