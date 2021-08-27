package com.fsck.k9.mail.internet

internal object FormatFlowedHelper {
    private const val TEXT_PLAIN = "text/plain"
    private const val HEADER_PARAM_FORMAT = "format"
    private const val HEADER_FORMAT_FLOWED = "flowed"
    private const val HEADER_PARAM_DELSP = "delsp"
    private const val HEADER_DELSP_YES = "yes"

    @JvmStatic
    fun checkFormatFlowed(contentTypeHeaderValue: String?): FormatFlowedResult {
        if (contentTypeHeaderValue == null) return negativeResult()

        val mimeValue = MimeParameterDecoder.decode(contentTypeHeaderValue)
        if (!MimeUtility.isSameMimeType(TEXT_PLAIN, mimeValue.value)) return negativeResult()

        val formatParameter = mimeValue.parameters[HEADER_PARAM_FORMAT]?.lowercase()
        if (formatParameter != HEADER_FORMAT_FLOWED) return negativeResult()

        val delSpParameter = mimeValue.parameters[HEADER_PARAM_DELSP]?.lowercase()

        return FormatFlowedResult(isFormatFlowed = true, isDelSp = delSpParameter == HEADER_DELSP_YES)
    }

    private fun negativeResult() = FormatFlowedResult(isFormatFlowed = false, isDelSp = false)
}

internal data class FormatFlowedResult(val isFormatFlowed: Boolean, val isDelSp: Boolean)
