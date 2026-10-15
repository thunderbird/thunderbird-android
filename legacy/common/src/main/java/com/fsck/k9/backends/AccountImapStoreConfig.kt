package com.fsck.k9.backends

import com.fsck.k9.mail.store.imap.ImapClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreConfig
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

/**
 * [ImapStoreConfig] that reads the latest account settings on every call.
 *
 * The IMAP store lives as long as the cached backend, so settings changed in the meantime (e.g. "subscribed folders
 * only") must not be read from a copy of the account taken when the backend was created.
 */
internal class AccountImapStoreConfig(
    private val accountManager: LegacyAccountManager,
    private val accountId: AccountId,
    private val clientInfo: ImapClientInfo,
) : ImapStoreConfig {
    override val logLabel: String
        get() = accountId.toString()

    override fun isSubscribedFoldersOnly(): Boolean = account?.isSubscribedFoldersOnly ?: false

    override fun isExpungeImmediately(): Boolean = account?.expungePolicy == Expunge.EXPUNGE_IMMEDIATELY

    override fun clientInfo(): ImapClientInfo = clientInfo

    private val account: LegacyAccount?
        get() = accountManager.findById(accountId)
}
