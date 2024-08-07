package com.fsck.k9.controller

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.FolderMode
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.search.ConditionsTreeNode
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.Preferences
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val ACCOUNT_UUID = "irrelevant"
private const val UNREAD_COUNT = 2
private const val STARRED_COUNT = 3

class DefaultMessageCountsProviderTest {
    private val preferences = mock<Preferences>()
    private val account = Account(ACCOUNT_UUID)
    private val messageStore = mock<ListenableMessageStore> {
        on { getUnreadMessageCount(anyOrNull<ConditionsTreeNode>()) } doReturn UNREAD_COUNT
        on { getStarredMessageCount(anyOrNull()) } doReturn STARRED_COUNT
    }
    private val messageStoreManager = mock<MessageStoreManager> {
        on { getMessageStore(account) } doReturn messageStore
    }

    private val messageCountsProvider = DefaultMessageCountsProvider(preferences, messageStoreManager)

    @Test
    fun `getMessageCounts() without any special folders and displayMode = ALL`() {
        account.inboxFolderId = null
        account.trashFolderId = null
        account.draftsFolderId = null
        account.spamFolderId = null
        account.outboxFolderId = null
        account.sentFolderId = null
        account.folderDisplayMode = FolderMode.ALL

        val messageCounts = messageCountsProvider.getMessageCounts(account)

        assertThat(messageCounts.unread).isEqualTo(UNREAD_COUNT)
        assertThat(messageCounts.starred).isEqualTo(STARRED_COUNT)
    }
}
