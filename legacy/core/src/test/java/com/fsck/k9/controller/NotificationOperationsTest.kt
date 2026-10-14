package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.notification.NotificationController
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class NotificationOperationsTest {
    private val firstAccount = createAccount(inboxFolderId = 10, spamFolderId = 12)
    private val secondAccount = createAccount(inboxFolderId = 20, spamFolderId = null)
    private val accountWithoutInbox = createAccount(inboxFolderId = null, spamFolderId = 32)

    private val selectors = mutableMapOf<AccountId, (List<MessageReference>) -> List<MessageReference>>()
    private val notificationController = mock<NotificationController> {
        on { clearNewMailNotifications(any(), any<(List<MessageReference>) -> List<MessageReference>>()) } doAnswer {
            selectors[it.getArgument(0)] = it.getArgument(1)
        }
    }

    private val testSubject = NotificationOperations(
        notificationController = notificationController,
        accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount, accountWithoutInbox)),
        outboxFolderManager = FakeOutboxFolderManager(),
    )

    @Test
    fun `clearing the unified inbox should clear notifications of each account's inbox only`() {
        // Act
        testSubject.clearNotifications(LocalMessageSearch.createUnifiedSearch(FolderRole.INBOX))

        // Assert
        assertThat(selectors.keys).isEqualTo(setOf(firstAccount.id, secondAccount.id))
        val firstAccountMessages = listOf(firstAccount.message(folderId = 10), firstAccount.message(folderId = 11))
        assertThat(selectors.getValue(firstAccount.id)(firstAccountMessages))
            .containsExactly(firstAccount.message(folderId = 10))
        val secondAccountMessages = listOf(secondAccount.message(folderId = 10), secondAccount.message(folderId = 20))
        assertThat(selectors.getValue(secondAccount.id)(secondAccountMessages))
            .containsExactly(secondAccount.message(folderId = 20))
    }

    @Test
    fun `clearing a unified folder should clear notifications of each account's folder with that role only`() {
        // Act
        testSubject.clearNotifications(LocalMessageSearch.createUnifiedSearch(FolderRole.SPAM))

        // Assert
        assertThat(selectors.keys).isEqualTo(setOf(firstAccount.id, accountWithoutInbox.id))
        val firstAccountMessages = listOf(firstAccount.message(folderId = 10), firstAccount.message(folderId = 12))
        assertThat(selectors.getValue(firstAccount.id)(firstAccountMessages))
            .containsExactly(firstAccount.message(folderId = 12))
        val otherAccountMessages = listOf(accountWithoutInbox.message(folderId = 32))
        assertThat(selectors.getValue(accountWithoutInbox.id)(otherAccountMessages))
            .containsExactly(accountWithoutInbox.message(folderId = 32))
    }

    private fun createAccount(inboxFolderId: Long?, spamFolderId: Long?) =
        FakeLegacyAccount.create(id = AccountIdFactory.create())
            .copy(inboxFolderId = inboxFolderId, spamFolderId = spamFolderId)

    private fun LegacyAccount.message(folderId: Long) = MessageReference(id, folderId, uid = "uid-$folderId")
}
