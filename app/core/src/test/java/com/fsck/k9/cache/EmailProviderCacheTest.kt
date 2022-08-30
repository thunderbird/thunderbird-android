package com.fsck.k9.cache

import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageListRepository
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val MESSAGE_ID = 1L
private const val FOLDER_ID = 2L

class EmailProviderCacheTest {
    private val localFolder = mock<LocalFolder> {
        on { databaseId } doReturn FOLDER_ID
    }

    private val localMessage = mock<LocalMessage> {
        on { databaseId } doReturn MESSAGE_ID
        on { folder } doReturn localFolder
    }

    private val cache = EmailProviderCache.getCache(UUID.randomUUID().toString())

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { mock<MessageListRepository>() }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `getCache() returns different cache for each UUID`() {
        val cache = EmailProviderCache.getCache("u001")

        val cache2 = EmailProviderCache.getCache("u002")

        assertThat(cache2).isNotSameInstanceAs(cache)
    }

    @Test
    fun `getCache() returns same cache for the same UUID`() {
        val cache = EmailProviderCache.getCache("u001")

        val cache2 = EmailProviderCache.getCache("u001")

        assertThat(cache2).isSameInstanceAs(cache)
    }

    @Test
    fun `getValueForMessage() returns value set for message`() {
        cache.setValueForMessages(listOf(1L), "subject", "Subject")

        val result = cache.getValueForMessage(1L, "subject")

        assertThat(result).isEqualTo("Subject")
    }

    @Test
    fun `getValueForUnknownMessage() returns null`() {
        val result = cache.getValueForMessage(1L, "subject")

        assertThat(result).isNull()
    }

    @Test
    fun `getValueForUnknownMessage() returns null when removed`() {
        cache.setValueForMessages(listOf(1L), "subject", "Subject")
        cache.removeValueForMessages(listOf(1L), "subject")

        val result = cache.getValueForMessage(1L, "subject")

        assertThat(result).isNull()
    }

    @Test
    fun `getValueForThread() returns value set for thread`() {
        cache.setValueForThreads(listOf(1L), "subject", "Subject")

        val result = cache.getValueForThread(1L, "subject")

        assertThat(result).isEqualTo("Subject")
    }

    @Test
    fun `getValueForUnknownThread() returns null`() {
        val result = cache.getValueForThread(1L, "subject")

        assertThat(result).isNull()
    }

    @Test
    fun `getValueForUnknownThread() returns null when removed`() {
        cache.setValueForThreads(listOf(1L), "subject", "Subject")
        cache.removeValueForThreads(listOf(1L), "subject")

        val result = cache.getValueForThread(1L, "subject")

        assertThat(result).isNull()
    }

    @Test
    fun `isMessageHidden() returns true for hidden message`() {
        cache.hideMessages(listOf(localMessage))

        val result = cache.isMessageHidden(MESSAGE_ID, FOLDER_ID)

        assertThat(result).isTrue()
    }

    @Test
    fun `isMessageHidden() returns false for unknown message`() {
        val result = cache.isMessageHidden(MESSAGE_ID, FOLDER_ID)

        assertThat(result).isFalse()
    }

    @Test
    fun `isMessageHidden() returns false for unhidden message`() {
        cache.hideMessages(listOf(localMessage))
        cache.unhideMessages(listOf(localMessage))

        val result = cache.isMessageHidden(MESSAGE_ID, FOLDER_ID)

        assertThat(result).isFalse()
    }
}
