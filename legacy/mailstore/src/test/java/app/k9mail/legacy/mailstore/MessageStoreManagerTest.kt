package app.k9mail.legacy.mailstore

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import net.thunderbird.feature.account.AccountIdFactory
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class MessageStoreManagerTest {

    private val accountId = AccountIdFactory.create()
    private val messageStore1 = mock<ListenableMessageStore>(name = "messageStore1")
    private val messageStore2 = mock<ListenableMessageStore>(name = "messageStore2")
    private val messageStoreFactory = mock<MessageStoreFactory> {
        on { create(accountId) } doReturn messageStore1 doReturn messageStore2
    }

    @Test
    fun `MessageStore instance is reused`() {
        val messageStoreManager = MessageStoreManager(messageStoreFactory)

        assertThat(messageStoreManager.getMessageStore(accountId)).isSameInstanceAs(messageStore1)
        assertThat(messageStoreManager.getMessageStore(accountId)).isSameInstanceAs(messageStore1)
    }

    @Test
    fun `MessageStore instance is removed when removeMessageStore is called`() {
        val messageStoreManager = MessageStoreManager(messageStoreFactory)

        assertThat(messageStoreManager.getMessageStore(accountId)).isSameInstanceAs(messageStore1)

        messageStoreManager.removeMessageStore(accountId)

        assertThat(messageStoreManager.getMessageStore(accountId)).isSameInstanceAs(messageStore2)
    }
}
