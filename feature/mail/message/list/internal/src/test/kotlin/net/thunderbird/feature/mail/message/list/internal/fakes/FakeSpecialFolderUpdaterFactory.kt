package net.thunderbird.feature.mail.message.list.internal.fakes

import dev.mokkery.spy
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

internal class FakeSpecialFolderUpdaterFactory : SpecialFolderUpdater.Factory<BaseAccount> {
    val specialFolderUpdater = spy<SpecialFolderUpdater>(FakeSpecialFolderUpdater())

    override fun create(account: BaseAccount): SpecialFolderUpdater = specialFolderUpdater
}

private open class FakeSpecialFolderUpdater : SpecialFolderUpdater {
    override fun updateSpecialFolders() = Unit

    override fun setSpecialFolder(
        type: FolderType,
        folderId: Long?,
        selection: SpecialFolderSelection,
    ) = Unit
}
