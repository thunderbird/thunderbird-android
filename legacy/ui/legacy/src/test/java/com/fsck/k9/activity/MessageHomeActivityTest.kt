package com.fsck.k9.activity

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.activity.MessageHomeActivity.DisplayMode
import org.junit.Test

class MessageHomeActivityTest {
    @Test
    fun `first launch with message reference shows message view`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = null,
            isRestoring = false,
            hasMessageView = false,
            hasMessageReference = true,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_VIEW)
    }

    @Test
    fun `first launch without message reference shows message list`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = null,
            isRestoring = false,
            hasMessageView = false,
            hasMessageReference = false,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_LIST)
    }

    @Test
    fun `restored message view falls back to message list when message view is missing`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = DisplayMode.MESSAGE_VIEW,
            isRestoring = true,
            hasMessageView = false,
            hasMessageReference = true,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_LIST)
    }

    @Test
    fun `restored message view remains selected when message view exists`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = DisplayMode.MESSAGE_VIEW,
            isRestoring = true,
            hasMessageView = true,
            hasMessageReference = false,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_VIEW)
    }

    @Test
    fun `restored message list remains selected when message view exists`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = DisplayMode.MESSAGE_LIST,
            isRestoring = true,
            hasMessageView = true,
            hasMessageReference = true,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_LIST)
    }

    @Test
    fun `restored split view ignores intent message reference when message view is missing`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = DisplayMode.SPLIT_VIEW,
            isRestoring = true,
            hasMessageView = false,
            hasMessageReference = true,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_LIST)
    }

    @Test
    fun `restored split view shows message view when message view exists`() {
        // Act
        val testSubject = MessageHomeActivity.resolveDisplayMode(
            savedMode = DisplayMode.SPLIT_VIEW,
            isRestoring = true,
            hasMessageView = true,
            hasMessageReference = false,
        )

        // Assert
        assertThat(testSubject).isEqualTo(DisplayMode.MESSAGE_VIEW)
    }
}
