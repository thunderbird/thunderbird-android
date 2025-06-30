package app.k9mail.backend.demo

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MimeMessage
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

internal class DemoDataLoader {

    @OptIn(ExperimentalSerializationApi::class)
    fun loadFolders(): DemoFolders {
        return getResourceAsStream("/contents.json").use { inputStream ->
            Json.decodeFromStream<DemoFolders>(inputStream)
        }
    }

    fun loadMessage(folderServerId: String, messageServerId: String): Message {
        return getResourceAsStream("/$folderServerId/$messageServerId.eml").use { inputStream ->
            MimeMessage.parseMimeMessage(inputStream, false).apply {
                uid = messageServerId
            }
        }
    }

    private fun getResourceAsStream(name: String): InputStream {
        return this.javaClass.getResourceAsStream(name) ?: error("Resource '$name' not found")
    }
}
