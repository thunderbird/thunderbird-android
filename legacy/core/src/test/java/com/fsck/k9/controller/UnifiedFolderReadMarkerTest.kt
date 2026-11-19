package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.fsck.k9.FakeLegacyAccount
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test

class UnifiedFolderReadMarkerTest {
    private val markedFolders = mutableListOf<Pair<AccountId, Long>>()

    @Test
    fun `should mark each account's folder with the role as read and skip accounts without one`() {
        val firstAccount = createAccount(inboxFolderId = 10)
        val secondAccount = createAccount(inboxFolderId = 20)
        val accountWithoutInbox = createAccount(inboxFolderId = null)
        val testSubject = createTestSubject(firstAccount, secondAccount, accountWithoutInbox)

        testSubject.markAllAsRead(FolderRole.INBOX)

        assertThat(markedFolders).containsExactly(
            firstAccount.id to 10L,
            secondAccount.id to 20L,
        )
    }

    @Test
    fun `should mark nothing when no account has a folder for the role`() {
        val testSubject = createTestSubject(createAccount(inboxFolderId = null))

        testSubject.markAllAsRead(FolderRole.INBOX)

        assertThat(markedFolders).isEmpty()
    }

    private fun createTestSubject(vararg accounts: LegacyAccount): UnifiedFolderReadMarker {
        return UnifiedFolderReadMarker(
            accountManager = FakeLegacyAccountManager(accounts.toList()),
            outboxFolderManager = FakeOutboxFolderManager(),
            markAllMessagesRead = { accountId, folderId -> markedFolders += accountId to folderId },
        )
    }

    private fun createAccount(inboxFolderId: Long?): LegacyAccount =
        FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(inboxFolderId = inboxFolderId)
}
