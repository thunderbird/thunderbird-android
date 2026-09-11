package com.fsck.k9.activity.setup.signature

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

internal class SignaturePreviewLoadStateTest {

    @Test
    fun `should load first signature`() {
        // Arrange
        val testSubject = SignaturePreviewLoadState()

        // Act
        val shouldLoad = testSubject.shouldLoad("Signature")

        // Assert
        assertThat(shouldLoad).isTrue()
    }

    @Test
    fun `should not reload unchanged signature`() {
        // Arrange
        val testSubject = SignaturePreviewLoadState()
        testSubject.shouldLoad("Signature")

        // Act
        val shouldLoad = testSubject.shouldLoad("Signature")

        // Assert
        assertThat(shouldLoad).isFalse()
    }

    @Test
    fun `should load changed signature`() {
        // Arrange
        val testSubject = SignaturePreviewLoadState()
        testSubject.shouldLoad("First signature")

        // Act
        val shouldLoad = testSubject.shouldLoad("Second signature")

        // Assert
        assertThat(shouldLoad).isTrue()
    }
}
