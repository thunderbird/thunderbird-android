package app.k9mail.legacy.mailstore

import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.AccountId

class MessageStoreManager(
    private val accountManager: LegacyAccountDtoManager,
    private val messageStoreFactory: MessageStoreFactory,
) {
    private val messageStores = ConcurrentHashMap<AccountId, ListenableMessageStore>()

    init {
        accountManager.addAccountRemovedListener { accountId ->
            removeMessageStore(accountId)
        }
    }

    fun getMessageStore(accountId: AccountId): ListenableMessageStore {
        val account = accountManager.getAccount(accountId.toString()) ?: error("Account not found: $accountId")
        return getMessageStore(account)
    }

    fun getMessageStore(accountUuid: String): ListenableMessageStore {
        val account = accountManager.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
        return getMessageStore(account)
    }

    fun getMessageStore(account: LegacyAccountDto): ListenableMessageStore {
        return messageStores.getOrPut(account.id) { messageStoreFactory.create(account) }
    }

    private fun removeMessageStore(id: AccountId) {
        messageStores.remove(id)
    }
}
