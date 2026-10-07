package net.thunderbird.feature.mail.message.list.internal

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.BeforeTest
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccount
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccountManager

class LocalDeleteOperationDeciderTest {

    private val accountId = AccountIdFactory.create()
    private val accountManager = FakeLegacyAccountManager(mutableListOf())
    private val localDeleteOperationDecider = DefaultLocalDeleteOperationDecider(accountManager)

    @BeforeTest
    fun setup() {
        accountManager.accounts.clear()
    }

    @Test
    fun `delete message from trash folder`() {
        val account = createAccount(trashFolderId = TRASH_FOLDER_ID, spamFolderId = SPAM_FOLDER_ID)
        accountManager.accounts.add(account)

        val result = localDeleteOperationDecider.isDeleteImmediately(accountId, TRASH_FOLDER_ID)

        assertThat(result).isTrue()
    }

    @Test
    fun `delete message from spam folder`() {
        val account = createAccount(trashFolderId = TRASH_FOLDER_ID, spamFolderId = SPAM_FOLDER_ID)
        accountManager.accounts.add(account)

        val result = localDeleteOperationDecider.isDeleteImmediately(accountId, SPAM_FOLDER_ID)

        assertThat(result).isTrue()
    }

    @Test
    fun `delete message from regular folder`() {
        val account = createAccount(trashFolderId = TRASH_FOLDER_ID, spamFolderId = SPAM_FOLDER_ID)
        accountManager.accounts.add(account)

        val result = localDeleteOperationDecider.isDeleteImmediately(accountId, REGULAR_FOLDER_ID)

        assertThat(result).isFalse()
    }

    @Test
    fun `delete message from regular folder without trash folder configured`() {
        val account = createAccount(trashFolderId = null, spamFolderId = SPAM_FOLDER_ID)
        accountManager.accounts.add(account)

        val result = localDeleteOperationDecider.isDeleteImmediately(accountId, REGULAR_FOLDER_ID)

        assertThat(result).isTrue()
    }

    private fun createAccount(trashFolderId: Long?, spamFolderId: Long?): LegacyAccount {
        return FakeLegacyAccount(id = accountId).copy(
            trashFolderId = trashFolderId,
            spamFolderId = spamFolderId,
        )
    }

    companion object {
        private const val REGULAR_FOLDER_ID = 1L
        private const val SPAM_FOLDER_ID = 2L
        private const val TRASH_FOLDER_ID = 3L
    }
}
