package net.thunderbird.feature.mail.message.reader.api.ui

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

interface MessageReaderViewContract {
    abstract class ViewModel<TPart>(state: State<TPart>) :
        BaseViewModel<State<TPart>, Event<TPart>, Effect>(initialState = state)

    data class State<TPart>(val attachments: ImmutableList<AttachmentUiItem<TPart>> = persistentListOf())
    sealed interface Event<TPart> {
        data class UpdateAttachments<TPart>(
            val nonInlineAttachments: List<AttachmentViewInfoMapper.AttachmentMetadata<TPart>>,
            val extraNonInlineAttachments: List<AttachmentViewInfoMapper.AttachmentMetadata<TPart>>,
        ) : Event<TPart>
    }

    sealed interface Effect
}
