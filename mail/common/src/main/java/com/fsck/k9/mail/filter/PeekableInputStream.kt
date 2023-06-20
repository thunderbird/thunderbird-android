package com.fsck.k9.mail.filter

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/**
 * A filtering InputStream that allows single byte "peeks" without consuming the byte.
 *
 * The client of this stream can call `peek()` to see the next available byte in the stream and a subsequent read will
 * still return the peeked byte.
 */
class PeekableInputStream(private val inputStream: InputStream) : FilterInputStream(inputStream) {
    private var peeked = false
    private var peekedByte = 0

    @Throws(IOException::class)
    override fun read(): Int {
        return if (!peeked) {
            inputStream.read()
        } else {
            peeked = false
            peekedByte
        }
    }

    @Throws(IOException::class)
    fun peek(): Int {
        if (!peeked) {
            peekedByte = inputStream.read()
            peeked = true
        }

        return peekedByte
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return if (!peeked) {
            inputStream.read(buffer, offset, length)
        } else {
            buffer[offset] = peekedByte.toByte()
            peeked = false

            val numberOfBytesRead = inputStream.read(buffer, offset + 1, length - 1)
            if (numberOfBytesRead == -1) {
                1
            } else {
                numberOfBytesRead + 1
            }
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray): Int {
        return read(buffer, 0, buffer.size)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return if (!peeked) {
            inputStream.skip(n)
        } else if (n > 0) {
            peeked = false
            inputStream.skip(n - 1)
        } else {
            0
        }
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return if (!peeked) {
            inputStream.available()
        } else {
            1 + inputStream.available()
        }
    }

    override fun markSupported(): Boolean {
        // We're not using this mechanism. So it's not worth the effort to add support for mark() and reset().
        return false
    }

    override fun toString(): String {
        return "PeekableInputStream(in=%s, peeked=%b, peekedByte=%d)".format(
            Locale.ROOT,
            inputStream.toString(),
            peeked,
            peekedByte,
        )
    }
}
