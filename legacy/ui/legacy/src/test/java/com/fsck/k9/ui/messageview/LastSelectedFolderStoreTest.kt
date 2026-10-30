package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class LastSelectedFolderStoreTest {
    private val testSubject = LastSelectedFolderStore()

    @Test
    fun `get should return null when no folder was chosen`() {
        assertThat(testSubject.get(AccountIdFactory.create())).isNull()
    }

    @Test
    fun `get should return the folder last chosen for each account`() {
        // Arrange
        val accountId = AccountIdFactory.create()
        val otherAccountId = AccountIdFactory.create()

        // Act
        testSubject.set(accountId, FIRST_FOLDER_ID)
        testSubject.set(accountId, SECOND_FOLDER_ID)
        testSubject.set(otherAccountId, FIRST_FOLDER_ID)

        // Assert
        assertThat(testSubject.get(accountId)).isEqualTo(SECOND_FOLDER_ID)
        assertThat(testSubject.get(otherAccountId)).isEqualTo(FIRST_FOLDER_ID)
    }

    private companion object {
        const val FIRST_FOLDER_ID = 1L
        const val SECOND_FOLDER_ID = 2L
    }
}
