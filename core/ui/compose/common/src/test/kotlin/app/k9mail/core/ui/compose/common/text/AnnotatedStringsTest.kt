package app.k9mail.core.ui.compose.common.text

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AnnotatedStringsTest {
    @Test
    fun bold() {
        val input = "text"

        val result = input.bold()

        assertThat(result.toString()).isEqualTo(input)
        assertThat(result.spanStyles).containsExactly(
            Range(
                item = SpanStyle(fontWeight = FontWeight.Bold),
                start = 0,
                end = input.length,
            ),
        )
    }
}
