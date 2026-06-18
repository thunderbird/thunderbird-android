package net.thunderbird.feature.mail.message.reader.impl.ui

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.ui.MessageReaderViewContract

class MessageReaderViewModel(
    private val attachmentViewInfoMapper: AttachmentViewInfoMapper<MailPart>,
) : MessageReaderViewContract.ViewModel<MailPart>(state = MessageReaderViewContract.State()) {
    override fun event(event: MessageReaderViewContract.Event<MailPart>) {
        when (event) {
            is MessageReaderViewContract.Event.UpdateAttachments -> updateAttachments(event)
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
