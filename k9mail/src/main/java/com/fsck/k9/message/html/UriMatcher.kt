package com.fsck.k9.message.html

import java.util.*

object UriMatcher {
    private val SUPPORTED_URIS = { httpUriParser: HttpUriParser ->
        mapOf(
                "ethereum:" to EthereumUriParser(),
                "bitcoin:" to BitcoinUriParser(),
                "http:" to httpUriParser,
                "https:" to httpUriParser,
                "rtsp:" to httpUriParser
        )
    }.invoke(HttpUriParser())

    private const val SCHEME_SEPARATORS = " (\\n<"
    private const val ALLOWED_SEPARATORS_PATTERN = "(?:^|[$SCHEME_SEPARATORS])"
    private val URI_SCHEME = Regex(
            "$ALLOWED_SEPARATORS_PATTERN(${ SUPPORTED_URIS.keys.joinToString("|") })",
            RegexOption.IGNORE_CASE
    )


    fun findUris(text: CharSequence): List<UriMatch> {
        return URI_SCHEME.findAll(text).map { matchResult ->
            val matchGroup = matchResult.groups[1]!!
            val startIndex = matchGroup.range.start
            val scheme = matchGroup.value.toLowerCase(Locale.ROOT)
            val parser = SUPPORTED_URIS[scheme] ?: throw AssertionError("Scheme not found: $scheme")

            parser.parseUri(text, startIndex)
        }.filterNotNull().toList()
    }
}
