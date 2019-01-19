package com.fsck.k9.preferences


import org.junit.Assert.*
import org.junit.Test


private const val TEST_STRING_KEY = "s"
private const val TEST_STRING_VALUE = "y"
private const val TEST_INT_KEY = "i"
private const val TEST_INT_VALUE = "4"
private const val TEST_STRING_DEFAULT = "z"
private const val TEST_INT_DEFAULT = 2
private val TEST_MAP = mapOf(
        TEST_STRING_KEY to TEST_STRING_VALUE,
        TEST_INT_KEY to TEST_INT_VALUE
)

class StorageTest {
    internal var storage = Storage()

    @Test
    fun isEmpty() {
        assertTrue(storage.isEmpty)
    }

    @Test
    fun isNotEmpty() {
        storage.replaceAll(TEST_MAP)
        assertFalse(storage.isEmpty)
    }

    @Test
    fun contains() {
        storage.replaceAll(TEST_MAP)
        assertTrue(storage.contains(TEST_STRING_KEY))
    }

    @Test
    fun getString() {
        storage.replaceAll(TEST_MAP)
        assertEquals(TEST_STRING_VALUE, storage.getString(TEST_STRING_KEY, TEST_STRING_DEFAULT))
    }

    @Test
    fun getString_default() {
        assertTrue(storage.isEmpty)
        assertEquals(TEST_STRING_DEFAULT, storage.getString(TEST_STRING_KEY, TEST_STRING_DEFAULT))
    }

    @Test
    fun getAll() {
        storage.replaceAll(TEST_MAP)
        assertFalse(storage.isEmpty)
        assertEquals(TEST_MAP, storage.all)
    }

    @Test
    fun replaceAll() {
        storage.replaceAll(TEST_MAP)
        storage.replaceAll(emptyMap())
        assertTrue(storage.isEmpty)
    }

    @Test
    fun getInteger() {
        storage.replaceAll(TEST_MAP)

        assertEquals(Integer.parseInt(TEST_INT_VALUE), storage.getInt(TEST_INT_KEY, TEST_INT_DEFAULT))
    }

    @Test
    fun getInteger_stringValue() {
        storage.replaceAll(TEST_MAP)

        // TODO is this good behavior?
        assertEquals(TEST_INT_DEFAULT, storage.getInt(TEST_STRING_KEY, TEST_INT_DEFAULT))
    }
}