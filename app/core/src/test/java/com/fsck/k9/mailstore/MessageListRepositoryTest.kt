package com.fsck.k9.mailstore

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"

class MessageListRepositoryTest {
    private val messageListRepository = MessageListRepository()

    @Test
    fun `adding and removing listener`() {
        var messageListChanged = 0
        val listener = MessageListChangedListener {
            messageListChanged++
        }
        messageListRepository.addListener(ACCOUNT_UUID, listener)

        messageListRepository.notifyMessageListChanged(ACCOUNT_UUID)

        assertThat(messageListChanged).isEqualTo(1)

        messageListRepository.removeListener(listener)

        messageListRepository.notifyMessageListChanged(ACCOUNT_UUID)

        assertThat(messageListChanged).isEqualTo(1)
    }

    @Test
    fun `only notify listener when account UUID matches`() {
        var messageListChanged = 0
        val listener = MessageListChangedListener {
            messageListChanged++
        }
        messageListRepository.addListener(ACCOUNT_UUID, listener)

        messageListRepository.notifyMessageListChanged("otherAccountUuid")

        assertThat(messageListChanged).isEqualTo(0)
    }

    @Test
    fun `notifyMessageListChanged() without any listeners should not throw`() {
        messageListRepository.notifyMessageListChanged(ACCOUNT_UUID)
    }
}
