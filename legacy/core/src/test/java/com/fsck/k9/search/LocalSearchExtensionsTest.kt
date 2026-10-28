package com.fsck.k9.search

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.FakeLegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.Test

class LocalSearchExtensionsTest {

    @Test
    fun `unifiedFolderRole should return the role of a unified search`() {
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.SENT)

        assertThat(search.unifiedFolderRole).isEqualTo(FolderRole.SENT)
    }

    @Test
    fun `unifiedFolderRole should return null for an account search`() {
        val search = LocalMessageSearch()

        assertThat(search.unifiedFolderRole).isNull()
    }

    @Test
    fun `folderIdsFor should return each account's folder with the role and leave out accounts without one`() {
        // Arrange
        val firstAccount = createAccount(sentFolderId = 10)
        val secondAccount = createAccount(sentFolderId = 20)
        val accountWithoutSentFolder = createAccount(sentFolderId = null)
        val accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount, accountWithoutSentFolder))

        // Act
        val result = accountManager.folderIdsFor(FolderRole.SENT, FakeOutboxFolderManager())

        // Assert
        assertThat(result).isEqualTo(mapOf(firstAccount.id to 10L, secondAccount.id to 20L))
    }

    @Test
    fun `folderIdsFor should return each account's outbox`() {
        // Arrange
        val firstAccount = createAccount(sentFolderId = null)
        val secondAccount = createAccount(sentFolderId = null)
        val accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount))
        val outboxFolderManager = FakeOutboxFolderManager(
            outboxIdMapping = mutableMapOf(firstAccount.id to 5L, secondAccount.id to 6L),
        )

        // Act
        val result = accountManager.folderIdsFor(FolderRole.OUTBOX, outboxFolderManager)

        // Assert
        assertThat(result).isEqualTo(mapOf(firstAccount.id to 5L, secondAccount.id to 6L))
    }

    private fun createAccount(sentFolderId: Long?) =
        FakeLegacyAccount.create(id = AccountIdFactory.create()).copy(sentFolderId = sentFolderId)
}
