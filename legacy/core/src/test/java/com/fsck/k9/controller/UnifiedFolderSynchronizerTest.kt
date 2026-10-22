package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.containsExactly
import com.fsck.k9.FakeLegacyAccount
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test

class UnifiedFolderSynchronizerTest {
    private val firstAccount = createAccount(sentFolderId = 10)
    private val secondAccount = createAccount(sentFolderId = 20)
    private val folderSynchronizer = FakeFolderSynchronizer()

    private val testSubject = UnifiedFolderSynchronizer(
        accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount)),
        outboxFolderManager = FakeOutboxFolderManager(),
        folderSynchronizer = folderSynchronizer,
    )

    @Test
    fun `should synchronize each account's folder with the role and send its pending messages`() {
        testSubject.synchronize(FolderRole.SENT, listener = null)

        assertThat(folderSynchronizer.synchronizedFolders).containsExactly(
            firstAccount.id to LegacyFolderIdFactory.of(10),
            secondAccount.id to LegacyFolderIdFactory.of(20),
        )
        assertThat(folderSynchronizer.sentPendingMessages).containsExactly(firstAccount.id, secondAccount.id)
    }

    private fun createAccount(sentFolderId: Long): LegacyAccount =
        FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(sentFolderId = sentFolderId)
}
