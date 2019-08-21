package com.fsck.k9.backend.eas

import com.fsck.k9.mail.Message
import com.nhaarman.mockito_kotlin.*
import org.junit.Test
import java.io.IOException

class MessageSendTest {
    private val client = mock<EasClient>()
    private val provisionManager = mock<EasProvisionManager>()

    @Test
    fun sendMessage_shouldSendMessage() {
        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = MessageSendCommand(client, provisionManager)

        val message = mock<Message>()

        cut.sendMessage(message)

        verify(client).sendMessage(message)
    }

    @Test(expected = IOException::class)
    fun sendMessage_clientException_shouldThrow() {
        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = MessageSendCommand(client, provisionManager)

        val message = mock<Message>()

        whenever(client.sendMessage(any())).thenAnswer { throw IOException() }

        cut.sendMessage(message)
    }
}
