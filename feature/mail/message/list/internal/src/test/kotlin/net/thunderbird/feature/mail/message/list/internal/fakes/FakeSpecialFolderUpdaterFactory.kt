package net.thunderbird.feature.mail.message.list.internal.fakes

import dev.mokkery.spy
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

internal class FakeSpecialFolderUpdaterFactory : SpecialFolderUpdater.Factory {
    val specialFolderUpdater = spy<SpecialFolderUpdater>(FakeSpecialFolderUpdater())

    override fun create(accountId: AccountId): SpecialFolderUpdater = specialFolderUpdater
}

private open class FakeSpecialFolderUpdater : SpecialFolderUpdater {
    override fun updateSpecialFolders() = Unit

    override fun setSpecialFolder(
        type: FolderType,
        folderId: Long?,
        selection: SpecialFolderSelection,
    ) = Unit
}
