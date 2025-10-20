package net.thunderbird.core.file

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * In-memory fake implementation of FileSystemManager for common tests.
 * Stores data in a simple map keyed by URI string.
 */
class FakeFileSystemManager : FileSystemManager {

    private val storage = mutableMapOf<String, ByteArray>()

    override fun openSink(uriString: String): RawSink? {
        return object : RawSink {
            private val collected = mutableListOf<Byte>()

            override fun write(source: Buffer, byteCount: Long) {
                // Read exactly byteCount bytes from source and collect
                val count = byteCount.toInt()
                repeat(count) {
                    if (source.size <= 0L) return
                    collected += source.readByte()
                }
            }

            override fun flush() {
                storage[uriString] = collected.toByteArray()
            }

            override fun close() {
                // ensure data is stored
                flush()
            }
        }
    }

    override fun openSource(uriString: String): RawSource? {
        val bytes = storage[uriString] ?: return null
        return object : RawSource {
            private val buffer = Buffer().apply { write(bytes) }
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                val toRead = minOf(byteCount, buffer.size)
                if (toRead <= 0L) return 0L
                sink.write(buffer, toRead)
                return toRead
            }

            override fun close() {
                // no-op
            }
        }
    }

    fun put(uriString: String, content: ByteArray) {
        storage[uriString] = content
    }

    fun get(uriString: String): ByteArray? = storage[uriString]
}
