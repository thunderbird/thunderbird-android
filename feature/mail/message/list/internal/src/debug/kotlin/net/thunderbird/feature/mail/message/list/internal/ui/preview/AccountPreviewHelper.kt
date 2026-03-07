package net.thunderbird.feature.mail.message.list.internal.ui.preview

import androidx.compose.ui.graphics.Color
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder

internal object AccountPreviewHelper {
    val account = Account(
        id = @OptIn(ExperimentalUuidApi::class) AccountId(Uuid.random()),
        color = Color.Blue,
    )

    val secondAccount = Account(
        id = @OptIn(ExperimentalUuidApi::class) AccountId(Uuid.random()),
        color = Color.Red,
    )

    val inboxFolder = Folder(
        id = "folder-inbox",
        account = account,
        name = "Inbox",
        type = FolderType.INBOX,
    )

    val sentFolder = Folder(
        id = "folder-sent",
        account = account,
        name = "Sent",
        type = FolderType.SENT,
    )
}
