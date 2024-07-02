package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.fail
import org.junit.Test

class ChunkedDatabaseOperationsTest {
    @Test(expected = IllegalArgumentException::class)
    fun `empty list`() {
        performChunkedOperation(
            arguments = emptyList(),
            argumentTransformation = Int::toString,
            operation = ::failCallback,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `chunkSize = 0`() {
        performChunkedOperation(
            arguments = listOf(1),
            argumentTransformation = Int::toString,
            chunkSize = 0,
            operation = ::failCallback,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `chunkSize = 1001`() {
        performChunkedOperation(
            arguments = listOf(1),
            argumentTransformation = Int::toString,
            chunkSize = 1001,
            operation = ::failCallback,
        )
    }

    @Test
    fun `single item`() {
        val chunks = mutableListOf<Pair<String, Array<String>>>()

        performChunkedOperation(
            arguments = listOf(1),
            argumentTransformation = Int::toString,
        ) { selectionSet, selectionArguments ->
            chunks.add(selectionSet to selectionArguments)
        }

        assertThat(chunks).hasSize(1)
        with(chunks.first()) {
            assertThat(first).isEqualTo("IN (?)")
            assertThat(second).isEqualTo(arrayOf("1"))
        }
    }

    @Test
    fun `2 items with chunk size of 1`() {
        val chunks = mutableListOf<Pair<String, Array<String>>>()

        performChunkedOperation(
            arguments = listOf(1, 2),
            argumentTransformation = Int::toString,
            chunkSize = 1,
        ) { selectionSet, selectionArguments ->
            chunks.add(selectionSet to selectionArguments)
        }

        assertThat(chunks).hasSize(2)
        with(chunks[0]) {
            assertThat(first).isEqualTo("IN (?)")
            assertThat(second).isEqualTo(arrayOf("1"))
        }
        with(chunks[1]) {
            assertThat(first).isEqualTo("IN (?)")
            assertThat(second).isEqualTo(arrayOf("2"))
        }
    }

    @Test
    fun `14 items with chunk size of 5`() {
        val chunks = mutableListOf<Pair<String, Array<String>>>()

        performChunkedOperation(
            arguments = (1..14).toList(),
            argumentTransformation = Int::toString,
            chunkSize = 5,
        ) { selectionSet, selectionArguments ->
            chunks.add(selectionSet to selectionArguments)
        }

        assertThat(chunks).hasSize(3)
        with(chunks[0]) {
            assertThat(first).isEqualTo("IN (?,?,?,?,?)")
            assertThat(second).isEqualTo(arrayOf("1", "2", "3", "4", "5"))
        }
        with(chunks[1]) {
            assertThat(first).isEqualTo("IN (?,?,?,?,?)")
            assertThat(second).isEqualTo(arrayOf("6", "7", "8", "9", "10"))
        }
        with(chunks[2]) {
            assertThat(first).isEqualTo("IN (?,?,?,?)")
            assertThat(second).isEqualTo(arrayOf("11", "12", "13", "14"))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun failCallback(selectionSet: String, selectionArguments: Array<String>) {
        fail("'operation' callback called when it shouldn't")
    }
}
