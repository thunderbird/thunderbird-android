package com.fsck.k9.message.html

object UriMatcher {
    private val SUPPORTED_URIS = run {
        val httpUriParser = HttpUriParser()
        mapOf(
            "ethereum:" to EthereumUriParser(),
            "bitcoin:" to BitcoinUriParser(),
            "http:" to httpUriParser,
            "https:" to httpUriParser,
            "rtsp:" to httpUriParser
        )
    }

    private const val SCHEME_SEPARATORS = "\\s(\\n<"
    private const val ALLOWED_SEPARATORS_PATTERN = "(?:^|[$SCHEME_SEPARATORS])"
    private val URI_SCHEME = Regex(
        "$ALLOWED_SEPARATORS_PATTERN(${ SUPPORTED_URIS.keys.joinToString("|") })",
        RegexOption.IGNORE_CASE
    )

    fun findUris(text: CharSequence): List<UriMatch> {
        return URI_SCHEME.findAll(text).map { matchResult ->
            val matchGroup = matchResult.groups[1]!!
            val startIndex = matchGroup.range.first
            val scheme = matchGroup.value.lowercase()
            val parser = SUPPORTED_URIS[scheme] ?: throw AssertionError("Scheme not found: $scheme")

            parser.parseUri(text, startIndex)
        }.filterNotNull().toList()
    }
}
