package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.Test

class ForwardPreparationTest {
    private val events = mutableListOf<String>()
    private val testSubject = ForwardPreparation(
        confirmDownload = { events += "confirm" },
        downloadMessage = { events += "download" },
        forward = { events += "forward" },
        showError = { events += "error" },
    )

    @Test
    fun `complete message forwards immediately`() {
        // Arrange
        // No pending request.

        // Act
        testSubject.start(false)

        // Assert
        assertThat(events).containsExactly("forward")
    }

    @Test
    fun `incomplete message waits for consent`() {
        // Arrange
        // No pending request.

        // Act
        testSubject.start(true)

        // Assert
        assertThat(events).containsExactly("confirm")
    }

    @Test
    fun `download waits for complete message before forwarding`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()

        // Act
        assertThat(events).containsExactly("confirm", "download")
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "download", "forward")
    }

    @Test
    fun `still incomplete after download reports failure and permits retry`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()

        // Act
        testSubject.onMessageLoaded(true)
        testSubject.onMessageLoaded(false)
        testSubject.start(true)

        // Assert
        assertThat(events).containsExactly("confirm", "download", "error", "confirm")
    }

    @Test
    fun `reload before consent never forwards`() {
        // Arrange
        testSubject.start(true)

        // Act
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm")
    }

    @Test
    fun `duplicate confirmation and completion forward only once`() {
        // Arrange
        testSubject.start(true)

        // Act
        testSubject.start(true)
        testSubject.download()
        testSubject.download()
        testSubject.onMessageLoaded(false)
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "download", "forward")
    }

    @Test
    fun `cancelled download ignores late completion`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()

        // Act
        testSubject.cancel()
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "download")
    }

    @Test
    fun `cancelled confirmation ignores late clicks`() {
        // Arrange
        testSubject.start(true)

        // Act
        testSubject.cancel()
        testSubject.download()
        testSubject.forwardWithoutDownloading()

        // Assert
        assertThat(events).containsExactly("confirm")
    }

    @Test
    fun `explicit forward as is skips download and forwards once`() {
        // Arrange
        testSubject.start(true)

        // Act
        testSubject.forwardWithoutDownloading()
        testSubject.forwardWithoutDownloading()
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "forward")
    }

    @Test
    fun `forward as is cannot bypass an ongoing download`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()

        // Act
        testSubject.forwardWithoutDownloading()

        // Assert
        assertThat(events).containsExactly("confirm", "download")
    }

    @Test
    fun `network failure cancellation allows another attempt`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()

        // Act
        testSubject.cancel()
        testSubject.start(true)
        testSubject.download()
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "download", "confirm", "download", "forward")
    }

    @Test
    fun `late completion cannot accept a new confirmation`() {
        // Arrange
        testSubject.start(true)
        testSubject.download()
        testSubject.cancel()

        // Act
        testSubject.start(true)
        testSubject.onMessageLoaded(false)

        // Assert
        assertThat(events).containsExactly("confirm", "download", "confirm")
    }
}
