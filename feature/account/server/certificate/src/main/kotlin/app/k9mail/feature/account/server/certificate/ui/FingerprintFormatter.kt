package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import okio.ByteString

/**
 * Format a certificate fingerprint.
 *
 * Outputs bytes as hexadecimal number, separated by `:`. Includes zero width space (U+200B) after colons to decrease
 * the chance of long lines being displayed with a line break in the middle of a byte.
 */
internal fun interface FingerprintFormatter {
    fun format(fingerprint: ByteString, separatorColor: Color): AnnotatedString
}

internal class DefaultFingerprintFormatter : FingerprintFormatter {
    override fun format(fingerprint: ByteString, separatorColor: Color): AnnotatedString {
        require(fingerprint.size > 0)

        return buildAnnotatedString {
            appendByteAsHexNumber(fingerprint[0])

            for (i in 1 until fingerprint.size) {
                appendSeparator(separatorColor)
                appendByteAsHexNumber(fingerprint[i])
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun AnnotatedString.Builder.appendByteAsHexNumber(byte: Byte) {
        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(byte.toHexString(format = HexFormat.UpperCase))
        }
    }

    private fun AnnotatedString.Builder.appendSeparator(separatorColor: Color) {
        withStyle(style = SpanStyle(color = separatorColor)) {
            append(":")
        }

        // Zero width space so long lines will be broken here and not in the middle of a byte value
        append('\u200B')
    }
}
