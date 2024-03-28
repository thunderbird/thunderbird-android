package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.Account
import com.fsck.k9.Account.DeletePolicy
import com.fsck.k9.preferences.FakeAccountManager
import java.util.UUID
import kotlin.test.Test

class PermanentDeleteCheckerTest {
    private val accountDefault = createAccount {
        spamFolderId = SPAM_FOLDER_ID
        trashFolderId = TRASH_FOLDER_ID
        deletePolicy = DeletePolicy.ON_DELETE
    }
    private val accountNoDelete = createAccount {
        spamFolderId = SPAM_FOLDER_ID
        trashFolderId = TRASH_FOLDER_ID
        deletePolicy = DeletePolicy.NEVER
    }
    private val accountAlwaysDelete = createAccount {
        spamFolderId = null
        trashFolderId = null
        deletePolicy = DeletePolicy.ON_DELETE
    }
    private val accountLocalTrash = createAccount {
        spamFolderId = null
        trashFolderId = LOCAL_FOLDER_ID
        deletePolicy = DeletePolicy.NEVER
    }
    private val accountManager = FakeAccountManager(
        accounts = listOf(accountDefault, accountNoDelete, accountAlwaysDelete, accountLocalTrash),
    )
    private val localFolderChecker = LocalFolderChecker { _, folderId ->
        folderId == LOCAL_FOLDER_ID
    }
    private val deleteOperationDecider = DeleteOperationDecider()
    private val permanentDeleteChecker = PermanentDeleteChecker(
        accountManager,
        localFolderChecker,
        deleteOperationDecider,
    )

    @Test
    fun `delete message from local folder with delete policy set to not delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountLocalTrash.uuid, folderId = LOCAL_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.All)
    }

    @Test
    fun `delete message from trash folder with delete policy set to delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.All)
    }

    @Test
    fun `delete message from trash folder with delete policy set to not delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    @Test
    fun `delete message from spam folder with delete policy set to delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.All)
    }

    @Test
    fun `delete message from spam folder with delete policy set to not delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    @Test
    fun `delete message from regular folder with delete policy set to delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    @Test
    fun `delete message from regular folder with delete policy set to not delete`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    @Test
    fun `delete messages from multiple accounts, none permanently`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    @Test
    fun `delete messages from multiple accounts, some permanently`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountDefault.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountDefault.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountNoDelete.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountLocalTrash.uuid, folderId = LOCAL_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.Some(permanentDeleteCount = 3))
    }

    @Test
    fun `delete messages from multiple accounts, all permanently`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountDefault.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountAlwaysDelete.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountAlwaysDelete.uuid, folderId = SPAM_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountAlwaysDelete.uuid, folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = accountLocalTrash.uuid, folderId = LOCAL_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.All)
    }

    @Test
    fun `skip unknown account`() {
        val messageReferences = listOf(
            MessageReference(accountUuid = accountDefault.uuid, folderId = REGULAR_FOLDER_ID, uid = "irrelevant"),
            MessageReference(accountUuid = "invalid", folderId = TRASH_FOLDER_ID, uid = "irrelevant"),
        )

        val result = permanentDeleteChecker.checkPermanentDelete(messageReferences)

        assertThat(result).isEqualTo(PermanentDeleteResult.None)
    }

    private fun createAccount(block: Account.() -> Unit): Account {
        return Account(uuid = UUID.randomUUID().toString()).apply(block)
    }

    companion object {
        private const val REGULAR_FOLDER_ID = 1L
        private const val SPAM_FOLDER_ID = 2L
        private const val TRASH_FOLDER_ID = 3L
        private const val LOCAL_FOLDER_ID = 4L
    }
}
