package app.k9mail.backend.demo

import app.k9mail.backend.demo.DemoHelper.createNewServerId
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal class CommandSendMessage(
    private val backendStorage: BackendStorage,
    private val demoStore: DemoStore,
) {

    fun sendMessage(message: Message) {
        val inboxServerId = demoStore.getInboxFolderId()
        val backendFolder = backendStorage.getFolder(inboxServerId)

        val newMessage = message.copy(uid = createNewServerId())
        backendFolder.saveMessage(newMessage, MessageDownloadState.FULL)
    }

    private fun Message.copy(uid: String): MimeMessage {
        val outputStream = ByteArrayOutputStream()
        writeTo(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        return MimeMessage.parseMimeMessage(inputStream, false).apply {
            this.uid = uid
        }
    }
}
