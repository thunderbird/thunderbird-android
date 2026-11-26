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
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test
import org.mockito.kotlin.any
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
    fun `getMessageCounts() should return zero counts for a removed account`() {
        // Arrange
        val messageCountsProvider = DefaultMessageCountsProvider(
            accountManager = FakeLegacyAccountManager(),
            messageStoreManager = messageStoreManager,
            messagingControllerRegistry = messagingControllerRegistry,
            outboxFolderManager = FakeOutboxFolderManager(),
            logger = TestLogger(),
        )

        // Act
        val messageCounts = messageCountsProvider.getMessageCounts(ACCOUNT_ID)

        // Assert
        assertThat(messageCounts).isEqualTo(MessageCounts(unread = 0, starred = 0))
    }

    @Test
    fun `getMessageCounts() for a unified search should count each account's own folder`() {
        // Arrange
        val firstAccount = FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(inboxFolderId = 10)
        val secondAccount = FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(inboxFolderId = 20)
        val accountWithoutInbox = FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(inboxFolderId = null)
        val recordedConditions = mutableMapOf<AccountId, String>()
        val messageStoreManager = mock<MessageStoreManager> {
            on { getMessageStore(any<AccountId>()) } doAnswer { invocation ->
                val accountId = invocation.getArgument<AccountId>(0)
                mock<ListenableMessageStore> {
                    on { getUnreadMessageCount(anyOrNull<SearchConditionTreeNode>()) } doAnswer {
                        recordedConditions[accountId] = it.getArgument<SearchConditionTreeNode?>(0).toString()
                        1
                    }
                }
            }
        }
        val messageCountsProvider = DefaultMessageCountsProvider(
            accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount, accountWithoutInbox)),
            messageStoreManager = messageStoreManager,
            messagingControllerRegistry = messagingControllerRegistry,
            outboxFolderManager = FakeOutboxFolderManager(),
            logger = TestLogger(),
        )

        // Act
        val result = messageCountsProvider.getMessageCounts(LocalMessageSearch.createUnifiedSearch(FolderRole.INBOX))

        // Assert
        assertThat(result.unread).isEqualTo(2)
        assertThat(recordedConditions).isEqualTo(
            mapOf(
                firstAccount.id to folderCondition(10).toString(),
                secondAccount.id to folderCondition(20).toString(),
            ),
        )
    }

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

    private fun folderCondition(folderId: Long) =
        SearchCondition(MessageSearchField.FOLDER, SearchAttribute.EQUALS, folderId.toString())
}
