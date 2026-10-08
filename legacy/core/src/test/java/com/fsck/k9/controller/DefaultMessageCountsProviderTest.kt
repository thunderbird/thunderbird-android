package com.fsck.k9.controller

import app.cash.turbine.test
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.MessagingListener
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.FakeLegacyAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val UNREAD_COUNT = 2
private const val STARRED_COUNT = 3

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageCountsProviderTest {

    private val account = FakeLegacyAccount.create(id = ACCOUNT_ID)
    private val accountManager = mock<LegacyAccountManager> {
        on { findById(ACCOUNT_ID) } doReturn account
        on { findAll() } doReturn listOf(account)
    }
    private val messageStore = mock<ListenableMessageStore> {
        on {
            getUnreadMessageCount(
                anyOrNull<SearchConditionTreeNode>(),
            )
        } doReturn UNREAD_COUNT
        on { getStarredMessageCount(anyOrNull()) } doReturn STARRED_COUNT
    }
    private val messageStoreManager = mock<MessageStoreManager> {
        on { getMessageStore(ACCOUNT_ID) } doReturn messageStore
    }

    private val messagingControllerRegistry = mock<MessagingControllerRegistry> {}

    private val messageCountsProvider = DefaultMessageCountsProvider(
        accountManager = accountManager,
        messageStoreManager = messageStoreManager,
        messagingControllerRegistry = messagingControllerRegistry,
        outboxFolderManager = FakeOutboxFolderManager(),
        logger = TestLogger(),
    )

    @Test
    fun `getMessageCounts() without any special folders`() {
        val accountWithoutSpecialFolders = account.copy(
            inboxFolderId = null,
            trashFolderId = null,
            draftsFolderId = null,
            spamFolderId = null,
            sentFolderId = null,
        )
        val accountManager = mock<LegacyAccountManager> {
            on { findById(ACCOUNT_ID) } doReturn accountWithoutSpecialFolders
            on { findAll() } doReturn listOf(accountWithoutSpecialFolders)
        }
        val messageCountsProvider = DefaultMessageCountsProvider(
            accountManager = accountManager,
            messageStoreManager = messageStoreManager,
            messagingControllerRegistry = messagingControllerRegistry,
            outboxFolderManager = FakeOutboxFolderManager(),
            logger = TestLogger(),
        )

        val messageCounts = messageCountsProvider.getMessageCounts(ACCOUNT_ID)

        assertThat(messageCounts.unread).isEqualTo(UNREAD_COUNT)
        assertThat(messageCounts.starred).isEqualTo(STARRED_COUNT)
    }

    @Test
    fun `getMessageCountsFlow should emit for every change`() = runTest {
        var currentListener: SimpleMessagingListener? = null
        val registry = object : MessagingControllerRegistry {
            override fun addListener(listener: MessagingListener) {
                currentListener = listener as SimpleMessagingListener
            }

            override fun removeListener(listener: MessagingListener) {
                currentListener = null
            }
        }
        var currentCount = 0
        val messageStore = mock<ListenableMessageStore> {
            on {
                getUnreadMessageCount(
                    anyOrNull<SearchConditionTreeNode>(),
                )
            } doAnswer { currentCount }
            on { getStarredMessageCount(anyOrNull()) } doAnswer { currentCount }
        }
        val messageStoreManager = mock<MessageStoreManager> {
            on { getMessageStore(ACCOUNT_ID) } doReturn messageStore
        }
        val testSubject = DefaultMessageCountsProvider(
            accountManager = accountManager,
            messageStoreManager = messageStoreManager,
            messagingControllerRegistry = registry,
            outboxFolderManager = FakeOutboxFolderManager(),
            logger = TestLogger(),
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        val search = LocalMessageSearch().apply {
            addAccountId(ACCOUNT_ID)
        }

        testSubject.getMessageCountsFlow(search).test {
            assertThat(awaitItem()).isEqualTo(MessageCounts(0, 0))
            currentCount = 1
            currentListener?.folderStatusChanged(ACCOUNT_ID, 0)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(MessageCounts(1, 1))
            currentCount = 2
            currentListener?.folderStatusChanged(ACCOUNT_ID, 0)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(MessageCounts(2, 2))
        }
    }
}
