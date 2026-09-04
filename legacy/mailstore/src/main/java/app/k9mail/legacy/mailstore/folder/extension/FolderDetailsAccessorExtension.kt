package app.k9mail.legacy.mailstore.folder.extension

import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderTypeMapper
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.FolderType

internal fun FolderDetailsAccessor.getFolderType(account: LegacyAccount, outboxFolderId: Long): FolderType =
    if (id == outboxFolderId) {
        FolderType.OUTBOX
    } else {
        FolderTypeMapper.folderTypeOf(account, id)
    }
