package net.thunderbird.app.common.feature.mail

import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.mail.account.api.BaseAccount

/**
 * Resolves the correct [RemoteFolderCreator] implementation based on the [BaseAccount] type.
 */
class RemoteFolderCreatorResolver(
    private val imapFactory: RemoteFolderCreator.Factory,
) : RemoteFolderCreator.Factory {
    override fun create(account: BaseAccount): RemoteFolderCreator {
        return when (account) {
            is LegacyAccountDto -> when (account.incomingServerSettings.type) {
                Protocols.IMAP -> imapFactory.create(account)
                else -> NoOpRemoteFolderCreator
            }
            is LegacyAccount -> when (account.incomingServerSettings.type) {
                Protocols.IMAP -> imapFactory.create(account)
                else -> NoOpRemoteFolderCreator
            }
            else -> NoOpRemoteFolderCreator
        }
    }
}
