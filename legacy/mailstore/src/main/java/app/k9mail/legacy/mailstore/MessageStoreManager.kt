package app.k9mail.legacy.mailstore

import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.feature.account.AccountId

class MessageStoreManager(
    private val messageStoreFactory: MessageStoreFactory,
) {
    private val messageStores = ConcurrentHashMap<AccountId, ListenableMessageStore>()

    fun getMessageStore(accountId: AccountId): ListenableMessageStore {
        return messageStores.getOrPut(accountId) { messageStoreFactory.create(accountId) }
    }

    fun removeMessageStore(id: AccountId) {
        messageStores.remove(id)
    }
}
