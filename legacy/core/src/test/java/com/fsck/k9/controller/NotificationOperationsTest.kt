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
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class NotificationOperationsTest {
    private val firstAccount = createAccount(inboxFolderId = 10)
    private val secondAccount = createAccount(inboxFolderId = 20)
    private val accountWithoutInbox = createAccount(inboxFolderId = null)

    private val selectors = mutableMapOf<AccountId, (List<MessageReference>) -> List<MessageReference>>()
    private val notificationController = mock<NotificationController> {
        on { clearNewMailNotifications(any(), any<(List<MessageReference>) -> List<MessageReference>>()) } doAnswer {
            selectors[it.getArgument(0)] = it.getArgument(1)
        }
    }

    private val testSubject = NotificationOperations(
        notificationController = notificationController,
        accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount, accountWithoutInbox)),
    )

    @Test
    fun `clearing the unified inbox should clear notifications of each account's inbox only`() {
        // Act
        testSubject.clearNotifications(LocalMessageSearch.createUnifiedSearch(FolderType.INBOX))

        // Assert
        assertThat(selectors.keys).isEqualTo(setOf(firstAccount.id, secondAccount.id))
        val firstAccountMessages = listOf(firstAccount.message(folderId = 10), firstAccount.message(folderId = 11))
        assertThat(selectors.getValue(firstAccount.id)(firstAccountMessages))
            .containsExactly(firstAccount.message(folderId = 10))
        val secondAccountMessages = listOf(secondAccount.message(folderId = 10), secondAccount.message(folderId = 20))
        assertThat(selectors.getValue(secondAccount.id)(secondAccountMessages))
            .containsExactly(secondAccount.message(folderId = 20))
    }

    private fun createAccount(inboxFolderId: Long?) =
        FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(inboxFolderId = inboxFolderId)

    private fun LegacyAccount.message(folderId: Long) = MessageReference(id, folderId, uid = "uid-$folderId")
}
