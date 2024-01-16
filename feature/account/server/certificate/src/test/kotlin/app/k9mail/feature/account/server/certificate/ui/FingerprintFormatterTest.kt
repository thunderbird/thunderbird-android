package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex

class FingerprintFormatterTest {
    private val formatter = DefaultFingerprintFormatter()

    @Test
    fun `simple fingerprint`() {
        val fingerprint = "0088FF".decodeHex()
        val separatorColor = Color.Cyan

        val result = formatter.format(fingerprint, separatorColor)

        assertThat(result).isEqualTo(
            buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("00")
                }
                withStyle(SpanStyle(color = separatorColor)) {
                    append(":")
                }
                append('\u200B')

                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("88")
                }
                withStyle(SpanStyle(color = separatorColor)) {
                    append(":")
                }
                append('\u200B')

                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("FF")
                }
            },
        )
    }
}
