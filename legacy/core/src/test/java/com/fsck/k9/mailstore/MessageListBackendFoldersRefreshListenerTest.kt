package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class MessageListBackendFoldersRefreshListenerTest {
    private val accountId = AccountIdFactory.create()
    private val messageListRepository = FakeMessageListRepository()

    private val testSubject = MessageListBackendFoldersRefreshListener(
        messageListRepository = messageListRepository,
        accountId = accountId,
    )

    @Test
    fun `should not notify message lists before the folder list refresh`() {
        testSubject.onBeforeFolderListRefresh()

        assertThat(messageListRepository.notifiedAccountIds).isEmpty()
    }

    @Test
    fun `should notify message lists of the account after the folder list refresh`() = runTest {
        testSubject.onAfterFolderListRefresh()

        assertThat(messageListRepository.notifiedAccountIds).containsExactly(accountId)
    }

    private class FakeMessageListRepository : MessageListRepository {
        val notifiedAccountIds = mutableListOf<AccountId>()

        override fun addListener(listener: MessageListChangedListener) = error("Not used in this test")

        override fun addListener(
            accountId: AccountId,
            listener: MessageListChangedListener,
        ) = error("Not used in this test")

        override fun removeListener(listener: MessageListChangedListener) = error("Not used in this test")

        override fun notifyMessageListChanged(accountId: AccountId) {
            notifiedAccountIds.add(accountId)
        }

        override fun <T> getMessages(
            accountId: AccountId,
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = error("Not used in this test")

        override fun <T> getThreadedMessages(
            accountId: AccountId,
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = error("Not used in this test")

        override fun <T> getThread(
            accountId: AccountId,
            threadId: Long,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = error("Not used in this test")
    }
}
