package com.fsck.k9.controller.command

import app.k9mail.core.testing.TestClock
import assertk.assertThat
import assertk.assertions.isInstanceOf
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.preferences.AccountManager
import kotlin.test.Test
import org.mockito.Mockito.mock

class ConcreteAccountCommandFactoryTest {

    @Test
    fun `createUpdateFolderListCommand should return UpdateFolderListCommand`() {
        // Given
        val accountManager = mock<AccountManager>()
        val backendManager = mock<BackendManager>()
        val clock = TestClock()
        val factory = ConcreteAccountCommandFactory(accountManager, backendManager, clock)

        // When
        val command = factory.createUpdateFolderListCommand("accountUuid")

        // Then
        assertThat(command).isInstanceOf(UpdateFolderListCommand::class.java)
    }
}
