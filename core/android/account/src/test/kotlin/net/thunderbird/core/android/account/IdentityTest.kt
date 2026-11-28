package net.thunderbird.core.android.account

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class IdentityTest {

    @Test
    fun `isHtmlSignaturePresent should return true when signature is HTML, in use and not empty`() {
        // Arrange
        val testSubject = createIdentity()

        // Act
        val result = testSubject.isHtmlSignaturePresent

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isHtmlSignaturePresent should return false when signature is plain, unused, null, or empty`() {
        // Arrange
        val identities = listOf(
            createIdentity(signatureIsHtml = false),
            createIdentity(signatureUse = false),
            createIdentity(signature = null),
            createIdentity(signature = ""),
        )

        // Act
        val anyHtmlSigPresent = identities.any { it.isHtmlSignaturePresent }

        // Assert
        assertThat(anyHtmlSigPresent).isFalse()
    }

    private fun createIdentity(
        signature: String? = "<b>Yours Truly</b>",
        signatureUse: Boolean = true,
        signatureIsHtml: Boolean = true,
    ) = Identity(
        signature = signature,
        signatureUse = signatureUse,
        signatureIsHtml = signatureIsHtml,
    )
}
