package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageListRepository
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import java.util.UUID
import net.thunderbird.core.common.mail.Flag
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

class MessageListCacheTest {
    private val localFolder = mock<LocalFolder> {
        on { databaseId } doReturn FOLDER_ID
    }

    private val localMessage = mock<LocalMessage> {
        on { databaseId } doReturn MESSAGE_ID
        on { folder } doReturn localFolder
    }

    private val cache = MessageListCache.getCache(UUID.randomUUID().toString())

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { mock<MessageListRepository>() }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `getCache() returns different cache for each UUID`() {
        val cache = MessageListCache.getCache("u001")

        val cache2 = MessageListCache.getCache("u002")

        assertThat(cache2).isNotSameInstanceAs(cache)
    }

    @Test
    fun `getCache() returns same cache for the same UUID`() {
        val cache = MessageListCache.getCache("u001")

        val cache2 = MessageListCache.getCache("u001")

        assertThat(cache2).isSameInstanceAs(cache)
    }

    @Test
    fun `getFlagForMessage() returns value set for message`() {
        cache.setFlagForMessages(listOf(1L), Flag.SEEN, true)

        val result = cache.getFlagForMessage(1L, Flag.SEEN)

        assertThat(result).isNotNull().isTrue()
    }

    @Test
    fun `getFlagForMessage() with unknown message ID returns null`() {
        val result = cache.getFlagForMessage(1L, Flag.SEEN)

        assertThat(result).isNull()
    }

    @Test
    fun `getFlagForMessage() returns null when removed`() {
        cache.setFlagForMessages(listOf(1L), Flag.FLAGGED, false)
        cache.removeFlagForMessages(listOf(1L), Flag.FLAGGED)

        val result = cache.getFlagForMessage(1L, Flag.FLAGGED)

        assertThat(result).isNull()
    }

    @Test
    fun `getFlagForThread() returns value set for thread`() {
        cache.setValueForThreads(listOf(1L), Flag.SEEN, false)

        val result = cache.getFlagForThread(1L, Flag.SEEN)

        assertThat(result).isNotNull().isFalse()
    }

    @Test
    fun `getFlagForThread() with unknown message ID returns null`() {
        val result = cache.getFlagForThread(1L, Flag.ANSWERED)

        assertThat(result).isNull()
    }

    @Test
    fun `getFlagForThread() returns null when removed`() {
        cache.setValueForThreads(listOf(1L), Flag.SEEN, true)
        cache.removeFlagForThreads(listOf(1L), Flag.SEEN)

        val result = cache.getFlagForThread(1L, Flag.SEEN)

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
