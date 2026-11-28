package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class IsPlainTest {

    @Test
    fun `should be plain for a string without annotations`() {
        // Arrange
        val testSubject = AnnotatedString(TEXT)

        // Act
        val result = testSubject.isPlain()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `should not be plain with spans`() {
        // Arrange
        val testSubject = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(TEXT) }
        }

        // Act
        val result = testSubject.isPlain()

        // Assert
        assertThat(result).isFalse()
    }

    private companion object {
        const val TEXT = "text"
        const val URL = "https://example.com"
    }
}
