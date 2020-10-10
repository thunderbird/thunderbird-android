package com.fsck.k9.mail.internet

object MimeHeaderEncoder {
    @JvmStatic
    fun encode(name: String, value: String): String {
        // TODO: Fold long text that provides enough opportunities for folding and doesn't contain any characters that
        //  need to be encoded.
        return if (hasToBeEncoded(name, value)) {
            EncoderUtil.encodeEncodedWord(value)
        } else {
            value
        }
    }

    private fun hasToBeEncoded(name: String, value: String): Boolean {
        return exceedsRecommendedLineLength(name, value) || charactersNeedEncoding(value)
    }

    private fun exceedsRecommendedLineLength(name: String, value: String): Boolean {
        return name.length + 2 /* colon + space */ + value.length > RECOMMENDED_MAX_LINE_LENGTH
    }

    private fun charactersNeedEncoding(text: String): Boolean {
        return text.any { !it.isVChar() && !it.isWspOrCrlf() }
    }
}
