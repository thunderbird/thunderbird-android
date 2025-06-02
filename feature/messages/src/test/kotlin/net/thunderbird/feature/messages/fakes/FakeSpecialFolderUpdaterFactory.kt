package net.thunderbird.feature.messages.fakes

import io.mockk.spyk
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

class FakeSpecialFolderUpdaterFactory : SpecialFolderUpdater.Factory<BaseAccount> {
    val specialFolderUpdater = spyk(
        object : SpecialFolderUpdater {
            override fun updateSpecialFolders() = Unit

            override fun setSpecialFolder(
                type: FolderType,
                folderId: Long?,
                selection: SpecialFolderSelection,
            ) = Unit
        },
    )

    override fun create(account: BaseAccount): SpecialFolderUpdater = specialFolderUpdater
}
