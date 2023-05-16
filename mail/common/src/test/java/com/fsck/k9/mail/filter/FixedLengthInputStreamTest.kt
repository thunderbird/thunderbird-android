package com.fsck.k9.mail.filter

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.InputStream
import kotlin.test.Test
import okio.blackholeSink
import okio.buffer
import okio.source

class FixedLengthInputStreamTest {
    @Test
    fun `reading stream should return data up to limit`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)

        val result = fixedLengthInputStream.readToString()

        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun `reading stream should not consume more than limit from underlying InputStream`() {
        val inputStream = "Hello world".byteInputStream()
        val fixedLengthInputStream = FixedLengthInputStream(inputStream, 5)

        fixedLengthInputStream.exhaust()

        assertThat(inputStream).readToString().isEqualTo(" world")
    }

    // TODO: Maybe this should throw. The underlying stream delivering less bytes than expected is most likely an error.
    @Test
    fun `reading stream with limit greater than number of bytes in underlying InputStream`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 100)

        val result = fixedLengthInputStream.readToString()

        assertThat(result).isEqualTo("Hello world")
    }

    @Test
    fun `read() with oversized ByteArray should return data up to limit`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello World", length = 6)
        val data = ByteArray(100)

        val result = fixedLengthInputStream.read(data)

        assertThat(result).isEqualTo(6)
        assertThat(data).all {
            slice(0 until 6).asString().isEqualTo("Hello ")
            slice(6 until 100).isEqualTo(ByteArray(100 - 6))
        }
    }

    @Test
    fun `read() with oversized ByteArray should not consume more than limit from underlying InputStream`() {
        val inputStream = "Hello World".byteInputStream()
        val fixedLengthInputStream = FixedLengthInputStream(inputStream, 6)

        fixedLengthInputStream.read(ByteArray(100))

        assertThat(inputStream).readToString().isEqualTo("World")
    }

    @Test
    fun `read() with ByteArray smaller than limit should consume size of ByteArray`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello World", length = 6)
        val data = ByteArray(5)

        val result = fixedLengthInputStream.read(data)

        assertThat(result).isEqualTo(5)
        assertThat(data).asString().isEqualTo("Hello")
    }

    @Test
    fun `read() with oversized ByteArray in middle of stream should return data up to limit`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello World", length = 6)
        fixedLengthInputStream.consumeBytes(5)
        val data = ByteArray(10)

        val result = fixedLengthInputStream.read(data)

        assertThat(result).isEqualTo(1)
        assertThat(data).all {
            slice(0 until 1).asString().isEqualTo(" ")
            slice(1 until 10).isEqualTo(ByteArray(10 - 1))
        }
    }

    @Test
    fun `read() with oversized ByteArray in middle of stream should not read more than limit from underlying stream`() {
        val inputStream = "Hello World".byteInputStream()
        val fixedLengthInputStream = FixedLengthInputStream(inputStream, 6)
        fixedLengthInputStream.consumeBytes(5)

        fixedLengthInputStream.read(ByteArray(10))

        assertThat(inputStream).readToString().isEqualTo("World")
    }

    @Test
    fun `read() at start of stream`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Word", length = 2)

        val result = fixedLengthInputStream.read()

        assertThat(result).isEqualTo('W'.code)
    }

    @Test
    fun `read() in middle of stream`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Word", length = 2)
        fixedLengthInputStream.consumeBytes(1)

        val result = fixedLengthInputStream.read()

        assertThat(result).isEqualTo('o'.code)
    }

    @Test
    fun `read() at end of stream should return -1`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.exhaust()

        val result = fixedLengthInputStream.read()

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `read(ByteArray) at end of stream should return -1`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.exhaust()

        val result = fixedLengthInputStream.read(ByteArray(2))

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `read(ByteArray) with offset at end of stream should return -1`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.exhaust()

        val result = fixedLengthInputStream.read(ByteArray(2), 0, 2)

        assertThat(result).isEqualTo(-1)
    }

    @Test
    fun `available() at start of stream`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `available() after partial read`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.read()

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `available() after partial multi-byte read`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.consumeBytes(2)

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `available() after stream has been exhausted`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.exhaust()

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `available() after skip()`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.guaranteedSkip(2)

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `available() after skip remaining`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)
        fixedLengthInputStream.skipRemaining()

        val result = fixedLengthInputStream.available()

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `skip() should consume bytes`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)

        fixedLengthInputStream.guaranteedSkip(2)

        assertThat(fixedLengthInputStream).readToString().isEqualTo("llo")
    }

    @Test
    fun `skipRemaining() should exhaust stream`() {
        val fixedLengthInputStream = createFixedLengthInputStream(data = "Hello world", length = 5)

        fixedLengthInputStream.skipRemaining()

        assertThat(fixedLengthInputStream).isExhausted()
    }

    @Test
    fun `skipRemaining() should not consume more than limit from underlying InputStream`() {
        val inputStream = "Hello World".byteInputStream()
        val fixedLengthInputStream = FixedLengthInputStream(inputStream, 6)

        fixedLengthInputStream.skipRemaining()

        assertThat(inputStream).readToString().isEqualTo("World")
    }

    private fun createFixedLengthInputStream(data: String, length: Int): FixedLengthInputStream {
        return FixedLengthInputStream(data.byteInputStream(), length)
    }

    private fun InputStream.guaranteedSkip(numberOfBytesToSkip: Int) {
        var remaining = numberOfBytesToSkip.toLong()
        while (remaining > 0) {
            remaining -= skip(remaining)
        }

        assertThat(remaining).isEqualTo(0L)
    }

    private fun InputStream.readToString(): String = source().buffer().readUtf8()

    private fun InputStream.exhaust() {
        source().buffer().readAll(blackholeSink())
    }

    private fun InputStream.consumeBytes(numberOfBytes: Int) {
        val numberOfBytesRead = read(ByteArray(numberOfBytes))

        assertThat(numberOfBytesRead).isEqualTo(numberOfBytes)
    }

    private fun Assert<InputStream>.isExhausted() = given { actual ->
        assertThat(actual.read()).isEqualTo(-1)
        assertThat(actual.available()).isEqualTo(0)
    }

    private fun Assert<InputStream>.readToString() = transform { it.readToString() }

    private fun Assert<ByteArray>.slice(range: IntRange) = transform { it.sliceArray(range) }

    private fun Assert<ByteArray>.asString() = transform { String(it) }
}
