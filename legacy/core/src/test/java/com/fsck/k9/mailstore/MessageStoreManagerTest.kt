package com.fsck.k9.mailstore

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.Account
import com.fsck.k9.AccountRemovedListener
import com.fsck.k9.preferences.AccountManager
import org.junit.Test
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MessageStoreManagerTest {
    private val account = Account("00000000-0000-4000-0000-000000000000")
    private val messageStore1 = mock<ListenableMessageStore>(name = "messageStore1")
    private val messageStore2 = mock<ListenableMessageStore>(name = "messageStore2")
    private val messageStoreFactory = mock<MessageStoreFactory> {
        on { create(account) } doReturn messageStore1 doReturn messageStore2
    }

    @Test
    fun `MessageStore instance is reused`() {
        val accountManager = mock<AccountManager>()
        val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)

        assertThat(messageStoreManager.getMessageStore(account)).isSameInstanceAs(messageStore1)
        assertThat(messageStoreManager.getMessageStore(account)).isSameInstanceAs(messageStore1)
    }

    @Test
    fun `MessageStore instance is removed when account is removed`() {
        val listenerCaptor = argumentCaptor<AccountRemovedListener>()
        val accountManager = mock<AccountManager> {
            doNothingOn { addAccountRemovedListener(listenerCaptor.capture()) }
        }
        val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)

        assertThat(messageStoreManager.getMessageStore(account)).isSameInstanceAs(messageStore1)

        listenerCaptor.firstValue.onAccountRemoved(account)

        assertThat(messageStoreManager.getMessageStore(account)).isSameInstanceAs(messageStore2)
    }

    private fun <T : Any> KStubbing<T>.doNothingOn(block: T.() -> Any) {
        doNothing().whenever(mock).block()
    }
}
