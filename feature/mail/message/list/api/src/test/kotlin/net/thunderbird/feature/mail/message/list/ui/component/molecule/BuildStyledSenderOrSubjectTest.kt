package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import org.junit.Test

class BuildStyledSenderOrSubjectTest {

    @Test
    fun `should apply sender styles after the prefix`() {
        // Arrange
        val senders = ComposedAddressUi(
            displayName = SENDER,
            displayNameStyles = persistentListOf(
                ComposedAddressStyle.Bold(start = 0, end = SEPARATOR_INDEX),
                ComposedAddressStyle.Regular(start = SEPARATOR_INDEX),
            ),
        )

        // Act
        val result = buildStyledSenderOrSubject(
            useSender = true,
            senders = senders,
            subject = AnnotatedString("subject"),
            prefix = AnnotatedString(PREFIX),
            forceRegularFontWeight = false,
        )

        // Assert
        assertThat(result.spanStyles).containsExactly(
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Bold),
                start = PREFIX.length,
                end = PREFIX.length + SEPARATOR_INDEX,
            ),
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Normal),
                start = PREFIX.length + SEPARATOR_INDEX,
                end = PREFIX.length + SENDER.length,
            ),
        )
    }

    @Test
    fun `should apply the forced regular weight after the prefix`() {
        // Arrange
        val senders = ComposedAddressUi(displayName = SENDER)

        // Act
        val result = buildStyledSenderOrSubject(
            useSender = true,
            senders = senders,
            subject = AnnotatedString("subject"),
            prefix = AnnotatedString(PREFIX),
            forceRegularFontWeight = true,
        )

        // Assert
        assertThat(result.spanStyles).containsExactly(
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Normal),
                start = PREFIX.length,
                end = PREFIX.length + SENDER.length,
            ),
        )
    }

    @Test
    fun `should apply sender styles from the start without a prefix`() {
        // Arrange
        val senders = ComposedAddressUi(
            displayName = SENDER,
            displayNameStyles = persistentListOf(ComposedAddressStyle.Bold(start = 0)),
        )

        // Act
        val result = buildStyledSenderOrSubject(
            useSender = true,
            senders = senders,
            subject = AnnotatedString("subject"),
            prefix = null,
            forceRegularFontWeight = false,
        )

        // Assert
        assertThat(result.spanStyles).containsExactly(
            AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), start = 0, end = SENDER.length),
        )
    }

    private companion object {
        const val PREFIX = "[icon] "
        const val SENDER = "Alice, Bob"
        val SEPARATOR_INDEX = SENDER.indexOf(',')
    }
}
