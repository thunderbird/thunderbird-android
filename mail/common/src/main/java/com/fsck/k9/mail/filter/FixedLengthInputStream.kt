package com.fsck.k9.mail.filter

import java.io.IOException
import java.io.InputStream
import java.util.Locale
import org.apache.commons.io.IOUtils

/**
 * A filtering InputStream that stops allowing reads after the given length has been read. This
 * is used to allow a client to read directly from an underlying protocol stream without reading
 * past where the protocol handler intended the client to read.
 */
class FixedLengthInputStream(
    private val inputStream: InputStream,
    private val length: Int,
) : InputStream() {
    private var numberOfBytesRead = 0

    // TODO: Call available() on underlying InputStream if remainingBytes() > 0
    @Throws(IOException::class)
    override fun available(): Int {
        return remainingBytes()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (remainingBytes() == 0) {
            return -1
        }

        val byte = inputStream.read()
        if (byte != -1) {
            numberOfBytesRead++
        }

        return byte
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, offset: Int, length: Int): Int {
        if (remainingBytes() == 0) {
            return -1
        }

        val byte = inputStream.read(b, offset, length.coerceAtMost(remainingBytes()))
        if (byte != -1) {
            numberOfBytesRead += byte
        }

        return byte
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val numberOfSkippedBytes = inputStream.skip(n.coerceAtMost(remainingBytes().toLong()))
        if (numberOfSkippedBytes > 0) {
            numberOfBytesRead += numberOfSkippedBytes.toInt()
        }

        return numberOfSkippedBytes
    }

    @Throws(IOException::class)
    fun skipRemaining() {
        IOUtils.skipFully(this, remainingBytes().toLong())
    }

    private fun remainingBytes(): Int {
        return length - numberOfBytesRead
    }

    override fun toString(): String {
        return String.format(Locale.ROOT, "FixedLengthInputStream(in=%s, length=%d)", inputStream.toString(), length)
    }
}
