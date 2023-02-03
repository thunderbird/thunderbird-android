@file:JvmName("PartExtensions")

package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Part

/**
 * Return the `charset` parameter value of this [Part]'s `Content-Type` header.
 */
val Part.charset: String?
    get() {
        val contentTypeHeader = this.contentType ?: return null
        val (_, parameters, duplicateParameters) = MimeParameterDecoder.decodeBasic(contentTypeHeader)
        return parameters["charset"] ?: extractNonConflictingCharsetValue(duplicateParameters)
    }

// If there are multiple "charset" parameters, but they all agree on the value, we use that value.
private fun extractNonConflictingCharsetValue(duplicateParameters: List<Pair<String, String>>): String? {
    val charsets = duplicateParameters.asSequence()
        .filter { (parameterName, _) -> parameterName == "charset" }
        .map { (_, charset) -> charset.lowercase() }
        .toSet()

    return if (charsets.size == 1) charsets.first() else null
}
