package com.fsck.k9.mail.store.imap

import com.beetstra.jutf7.CharsetProvider
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal class FolderNameCodec {
    private val modifiedUtf7Charset = CharsetProvider().charsetForName("X-RFC-3501")
    private val asciiCharset = StandardCharsets.US_ASCII
    var acceptUtf8Encoding = false

    fun encode(folderName: String): String {
        if (acceptUtf8Encoding) {
            return folderName
        }

        val byteBuffer = modifiedUtf7Charset.encode(folderName)
        val bytes = ByteArray(byteBuffer.limit())
        byteBuffer.get(bytes)

        return String(bytes, asciiCharset)
    }

    fun decode(encodedFolderName: String): String {
        if (acceptUtf8Encoding) {
            return encodedFolderName
        }

        val decoder = modifiedUtf7Charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        val byteBuffer = ByteBuffer.wrap(encodedFolderName.toByteArray(asciiCharset))
        val charBuffer = decoder.decode(byteBuffer)

        return charBuffer.toString()
    }
}
