package net.thunderbird.core.logging.file

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.io.Buffer
import kotlinx.io.RawSink

class FakeFileSystemManager : FileSystemManager {

    var exportedContent: String? = null
    private val outputStream = ByteArrayOutputStream()

    override fun openSink(uriString: String, mode: String): RawSink? {
        return object : RawSink {
            override fun write(source: Buffer, byteCount: Long) {
                val bytes = ByteArray(byteCount.toInt())

                for (i in 0 until byteCount.toInt()) {
                    bytes[i] = source.readByte()
                }

                outputStream.write(bytes)

                exportedContent = String(outputStream.toByteArray(), StandardCharsets.UTF_8)
            }

            override fun flush() = Unit

            override fun close() = Unit
        }
    }
}
