package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.util.UUID
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccount

class LocalDeleteOperationDeciderTest {
    private val localDeleteOperationDecider = LocalDeleteOperationDecider()
    private val account = LegacyAccount(UUID.randomUUID().toString()).apply {
        spamFolderId = SPAM_FOLDER_ID
        trashFolderId = TRASH_FOLDER_ID
    }

    @Test
    fun `delete message from trash folder`() {
        val result = localDeleteOperationDecider.isDeleteImmediately(account, TRASH_FOLDER_ID)

        assertThat(result).isTrue()
    }

    @Test
    fun `delete message from spam folder`() {
        val result = localDeleteOperationDecider.isDeleteImmediately(account, SPAM_FOLDER_ID)

        assertThat(result).isTrue()
    }

    @Test
    fun `delete message from regular folder`() {
        val result = localDeleteOperationDecider.isDeleteImmediately(account, REGULAR_FOLDER_ID)

        assertThat(result).isFalse()
    }

    @Test
    fun `delete message from regular folder without trash folder configured`() {
        account.trashFolderId = null

        val result = localDeleteOperationDecider.isDeleteImmediately(account, REGULAR_FOLDER_ID)

        assertThat(result).isTrue()
    }

    companion object {
        private const val REGULAR_FOLDER_ID = 1L
        private const val SPAM_FOLDER_ID = 2L
        private const val TRASH_FOLDER_ID = 3L
    }
}
