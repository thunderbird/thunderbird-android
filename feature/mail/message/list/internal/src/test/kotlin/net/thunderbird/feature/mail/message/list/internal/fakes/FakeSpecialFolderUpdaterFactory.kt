package net.thunderbird.feature.mail.message.list.internal.fakes

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

internal class FakeSpecialFolderUpdaterFactory : SpecialFolderUpdater.Factory {
    val specialFolderUpdater = FakeSpecialFolderUpdater()

    override fun create(accountId: AccountId): SpecialFolderUpdater = specialFolderUpdater
}

internal class FakeSpecialFolderUpdater : SpecialFolderUpdater {
    val setSpecialFolderCalls = mutableListOf<SetSpecialFolderCall>()
    var updateSpecialFoldersCalls = 0

    override fun updateSpecialFolders() {
        updateSpecialFoldersCalls += 1
    }

    override fun updateSpecialFoldersSync() = Unit

    override fun setSpecialFolder(
        type: FolderType,
        folderId: Long?,
        selection: SpecialFolderSelection,
    ) {
        setSpecialFolderCalls += SetSpecialFolderCall(
            type = type,
            folderId = folderId,
            selection = selection,
        )
    }

    data class SetSpecialFolderCall(
        val type: FolderType,
        val folderId: Long?,
        val selection: SpecialFolderSelection,
    )
}
