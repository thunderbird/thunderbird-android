package app.k9mail.legacy.mailstore

import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccountDto

class MessageStoreManager(
    private val accountManager: AccountManager,
    private val messageStoreFactory: MessageStoreFactory,
) {
    private val messageStores = ConcurrentHashMap<String, ListenableMessageStore>()

    init {
        accountManager.addAccountRemovedListener { account ->
            removeMessageStore(account.uuid)
        }
    }

    fun getMessageStore(accountUuid: String): ListenableMessageStore {
        val account = accountManager.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
        return getMessageStore(account)
    }

    fun getMessageStore(account: LegacyAccountDto): ListenableMessageStore {
        return messageStores.getOrPut(account.uuid) { messageStoreFactory.create(account) }
    }

    private fun removeMessageStore(accountUuid: String) {
        messageStores.remove(accountUuid)
    }
}
