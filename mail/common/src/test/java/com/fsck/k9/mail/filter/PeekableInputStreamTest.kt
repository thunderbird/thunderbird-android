package com.fsck.k9.mail.filter

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.InputStream
import kotlin.test.Test

class PeekableInputStreamTest {
    @Test
    fun `read() should read bytes`() {
        val inputBytes = "abc".toByteArray()
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())

        assertThat(peekableInputStream.read()).isEqualTo('a'.code)
        assertThat(peekableInputStream.read()).isEqualTo('b'.code)
        assertThat(peekableInputStream.read()).isEqualTo('c'.code)
        assertThat(peekableInputStream.read()).isEqualTo(-1)
    }

    @Test
    fun `read(ByteArray) should write bytes to array`() {
        val inputBytes = byteArrayOf(1, 2, 3)
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())
        val data = ByteArray(4)

        val numberOfBytesRead = peekableInputStream.read(data)

        assertThat(numberOfBytesRead).isEqualTo(3)
        assertThat(data).containsExactly(1, 2, 3, 0)
    }

    @Test
    fun `read(ByteArray, Int, Int) should write bytes to array`() {
        val inputBytes = byteArrayOf(1, 2, 3)
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())
        val data = ByteArray(5)

        val numberOfBytesRead = peekableInputStream.read(data, 1, 3)

        assertThat(numberOfBytesRead).isEqualTo(3)
        assertThat(data).containsExactly(0, 1, 2, 3, 0)
    }

    @Test
    fun `peek() before read()`() {
        val inputBytes = "ab".toByteArray()
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())

        assertThat(peekableInputStream.peek()).isEqualTo('a'.code)
        assertThat(peekableInputStream.read()).isEqualTo('a'.code)
        assertThat(peekableInputStream.peek()).isEqualTo('b'.code)
        assertThat(peekableInputStream.read()).isEqualTo('b'.code)
        assertThat(peekableInputStream.peek()).isEqualTo(-1)
        assertThat(peekableInputStream.read()).isEqualTo(-1)
    }

    @Test
    fun `peek() before read(ByteArray)`() {
        val inputBytes = byteArrayOf(1, 2, 3)
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())
        val data = ByteArray(4)

        val peeked = peekableInputStream.peek()

        assertThat(peeked).isEqualTo(1)

        val numberOfBytesRead = peekableInputStream.read(data)

        assertThat(numberOfBytesRead).isEqualTo(3)
        assertThat(data).containsExactly(1, 2, 3, 0)
    }

    @Test
    fun `peek() on last byte before read(ByteArray)`() {
        val inputBytes = byteArrayOf(1)
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())
        val data = ByteArray(1)

        val peeked = peekableInputStream.peek()

        assertThat(peeked).isEqualTo(1)

        val numberOfBytesRead = peekableInputStream.read(data)

        assertThat(numberOfBytesRead).isEqualTo(1)
        assertThat(data).containsExactly(1)
    }

    @Test
    fun `peek() before read(ByteArray, Int, Int)`() {
        val inputBytes = byteArrayOf(1, 2, 3)
        val peekableInputStream = PeekableInputStream(inputBytes.inputStream())
        val data = ByteArray(5)

        val peeked = peekableInputStream.peek()

        assertThat(peeked).isEqualTo(1)

        val numberOfBytesRead = peekableInputStream.read(data, 1, 3)

        assertThat(numberOfBytesRead).isEqualTo(3)
        assertThat(data).containsExactly(0, 1, 2, 3, 0)
    }

    @Test
    fun `close() should close wrapped InputStream`() {
        val wrappedInputStream = object : InputStream() {
            var isClosed = false

            override fun read(): Int {
                throw UnsupportedOperationException("not implemented")
            }

            override fun close() {
                isClosed = true
            }
        }
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        peekableInputStream.close()

        assertThat(wrappedInputStream.isClosed).isTrue()
    }

    @Test
    fun `read() after peek() should read same byte`() {
        val wrappedInputStream = "Test".byteInputStream()
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        val peeked = peekableInputStream.peek()

        assertThat(peeked).isEqualTo('T'.code)

        val read = peekableInputStream.read()

        assertThat(read).isEqualTo('T'.code)
    }

    @Test
    fun `skip() should skip correct amount of bytes`() {
        val wrappedInputStream = "Test".byteInputStream()
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        peekableInputStream.skip(2)

        assertThat(peekableInputStream.readToString()).isEqualTo("st")
    }

    @Test
    fun `skip() after peek() should skip correct amount of bytes`() {
        val wrappedInputStream = "Test".byteInputStream()
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        peekableInputStream.peek()
        peekableInputStream.skip(2)

        assertThat(peekableInputStream.readToString()).isEqualTo("st")
    }

    @Test
    fun `available() should return number of available bytes`() {
        val wrappedInputStream = "Test".byteInputStream()
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        val available = peekableInputStream.available()

        assertThat(available).isEqualTo(4)
    }

    @Test
    fun `available() after peek() should return number of available bytes`() {
        val wrappedInputStream = "Test".byteInputStream()
        val peekableInputStream = PeekableInputStream(wrappedInputStream)

        peekableInputStream.peek()
        val available = peekableInputStream.available()

        assertThat(available).isEqualTo(4)
    }

    @Test
    fun `markSupported() should return false`() {
        val peekableInputStream = PeekableInputStream("abc".byteInputStream())

        val result = peekableInputStream.markSupported()

        assertThat(result).isFalse()
    }

    private fun InputStream.readToString() = readBytes().decodeToString()
}
