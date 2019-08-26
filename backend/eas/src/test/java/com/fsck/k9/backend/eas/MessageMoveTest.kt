package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.MoveItem
import com.fsck.k9.backend.eas.dto.MoveItems
import com.fsck.k9.backend.eas.dto.MoveResponse
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class MessageMoveTest {
    private val client = mock<EasClient>()
    private val provisionManager = mock<EasProvisionManager>()

    @Test
    fun moveMessage_shouldMoveMessage() {
        whenever(provisionManager.ensureProvisioned<(() -> HashMap<String, String>)>(any())).thenAnswer { (it.getArgument(0) as (() -> HashMap<String, String>))() }
        whenever(client.moveItems(
                MoveItems(listOf(
                        MoveItem("id1", "colSrc", "colDst"),
                        MoveItem("id2", "colSrc", "colDst"),
                        MoveItem("id3", "colSrc", "colDst")
                ))
        )).thenReturn(MoveItems(response = listOf(
                MoveResponse("id3", 1),
                MoveResponse("id1", 3, "idNew1"),
                MoveResponse("id2", 3, "idNew2")
        )))

        val cut = MessageMoveCommand(client, provisionManager)

        val result = cut.moveMessages("colSrc", "colDst", listOf("id1", "id2", "id3"))

        assertEquals(result, hashMapOf("id1" to "idNew1", "id2" to "idNew2"))
    }

    @Test(expected = MessagingException::class)
    fun sendMessage_serverException_shouldThrow() {
        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }
        whenever(client.moveItems(any())).thenReturn(MoveItems())

        val cut = MessageMoveCommand(client, provisionManager)

        cut.moveMessages("colSrc", "colDst", listOf("id1", "id2", "id3"))
    }
}
