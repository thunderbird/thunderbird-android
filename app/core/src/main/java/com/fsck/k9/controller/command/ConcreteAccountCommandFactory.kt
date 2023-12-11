package com.fsck.k9.controller.command

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.preferences.AccountManager
import kotlinx.datetime.Clock

class ConcreteAccountCommandFactory(
    private val accountManager: AccountManager,
    private val backendManager: BackendManager,
    private val clock: Clock,
) : AccountCommandFactory {
    override fun createUpdateFolderListCommand(accountUuid: String): Command {
        return UpdateFolderListCommand(
            argument = UpdateFolderListArgument(accountUuid),
            accountManager = accountManager,
            backendManager = backendManager,
            clock = clock,
        )
    }
}
