package net.thunderbird.feature.mail.message.list.internal.ui.preview

import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.feature.mail.message.list.ui.state.MessageListFooter
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata

internal object MessageListMetadataPreviewHelper {

    val defaultMetadata = MessageListMetadata(
        folder = null,
        swipeActions = persistentMapOf(),
        sortCriteriaPerAccount = persistentMapOf(),
        activeMessage = null,
        isActive = false,
        footer = MessageListFooter(),
    )

    val inboxMetadata = defaultMetadata.copy(folder = AccountPreviewHelper.inboxFolder)

    val sentMetadata = defaultMetadata.copy(folder = AccountPreviewHelper.sentFolder)
}
