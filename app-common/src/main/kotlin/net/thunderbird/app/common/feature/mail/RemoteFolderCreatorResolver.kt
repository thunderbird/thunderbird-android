package net.thunderbird.app.common.feature.mail

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.BaseAccount

/**
 * Resolves the correct [RemoteFolderCreator] implementation based on the [BaseAccount] type.
 */
class RemoteFolderCreatorResolver(
    private val accountManager: LegacyAccountManager,
    private val imapFactory: ImapRemoteFolderCreatorFactory,
) : RemoteFolderCreator.Factory {
    override fun create(accountId: AccountId): RemoteFolderCreator {
        val account = runBlocking {
            accountManager.getById(accountId).firstOrNull()
        } ?: error("Account not found: $accountId")

        return when (account.incomingServerSettings.type) {
            Protocols.IMAP -> imapFactory.create(accountId)
            else -> NoOpRemoteFolderCreator
        }
    }
}
