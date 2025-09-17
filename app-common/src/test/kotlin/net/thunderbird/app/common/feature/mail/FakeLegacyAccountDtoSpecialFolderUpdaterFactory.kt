package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mailstore.LegacyAccountDtoSpecialFolderUpdaterFactory
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

internal class FakeLegacyAccountDtoSpecialFolderUpdaterFactory : LegacyAccountDtoSpecialFolderUpdaterFactory {
    var lastAccount: LegacyAccountDto? = null

    override fun create(account: LegacyAccountDto): SpecialFolderUpdater {
        lastAccount = account

        return object : SpecialFolderUpdater {
            override fun updateSpecialFolders() = Unit
            override fun setSpecialFolder(
                type: FolderType,
                folderId: Long?,
                selection: SpecialFolderSelection,
            ) = Unit
        }
    }
}
