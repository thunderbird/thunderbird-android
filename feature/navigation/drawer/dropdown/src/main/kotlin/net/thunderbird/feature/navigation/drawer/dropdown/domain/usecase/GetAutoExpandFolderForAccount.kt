package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

internal class GetAutoExpandFolderForAccount(
    private val accountManager: LegacyAccountDtoManager,
) : UseCase.GetAutoExpandFolderForAccount {

    override fun invoke(accountUuid: String): Long? {
        val account = accountManager.getAccount(accountUuid)
        return account?.autoExpandFolderId
    }
}
