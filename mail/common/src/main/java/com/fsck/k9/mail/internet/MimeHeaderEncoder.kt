package com.fsck.k9.mail.internet

import org.apache.james.mime4j.util.MimeUtil

object MimeHeaderEncoder {
    @JvmStatic
    fun encode(name: String, value: String): String {
        // TODO: Fold long text that provides enough opportunities for folding and doesn't contain any characters that
        //  need to be encoded.

        // Number of characters already used up on the first line (header field name + colon + space)
        val usedCharacters = name.length + COLON_PLUS_SPACE_LENGTH

        return if (hasToBeEncoded(value, usedCharacters)) {
            MimeUtil.fold(EncoderUtil.encodeEncodedWord(value), usedCharacters)
        } else {
            value
        }
    }

    private fun hasToBeEncoded(value: String, usedCharacters: Int): Boolean {
        return exceedsRecommendedLineLength(value, usedCharacters) || charactersNeedEncoding(value)
    }

    private fun exceedsRecommendedLineLength(value: String, usedCharacters: Int): Boolean {
        return usedCharacters + value.length > RECOMMENDED_MAX_LINE_LENGTH
    }

    private fun charactersNeedEncoding(text: String): Boolean {
        return text.any { !it.isVChar() && !it.isWspOrCrlf() }
    }

    private const val COLON_PLUS_SPACE_LENGTH = 2
}
