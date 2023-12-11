package com.fsck.k9.controller.command

interface AccountCommandFactory {
    fun createUpdateFolderListCommand(accountUuid: String): Command
}
