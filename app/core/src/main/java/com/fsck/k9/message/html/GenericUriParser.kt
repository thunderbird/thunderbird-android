package com.fsck.k9.message.html

import java.util.regex.Pattern

/**
 * Matches the URI generic syntax.
 *
 * See [RFC 3986](https://www.rfc-editor.org/rfc/rfc3986).
 */
class GenericUriParser : UriParser {
    override fun parseUri(text: CharSequence, startPos: Int): UriMatch? {
        require(startPos in text.indices) { "Invalid 'startPos' value" }

        val matcher = PATTERN.matcher(text)
        if (!matcher.find(startPos) || matcher.start() != startPos) return null

        val startIndex = matcher.start()
        val endIndex = matcher.end()
        val uri = text.subSequence(startIndex, endIndex)

        return UriMatch(startIndex, endIndex, uri)
    }

    companion object {
        private const val SCHEME = "[a-zA-Z][a-zA-Z0-9+.\\-]*"
        private const val AUTHORITY = "[a-zA-Z0-9\\-._~%!\$&'()*+,;=:\\[\\]@]*"
        private const val PATH = "[a-zA-Z0-9\\-._~%!\$&'()*+,;=:@/]*"
        private const val QUERY = "[a-zA-Z0-9\\-._~%!\$&'()*+,;=:@/?]*"
        private const val FRAGMENT = "[a-zA-Z0-9\\-._~%!\$&'()*+,;=:@/?]*"

        // This regular expression matches more than allowed by the generic URI syntax. So we might end up linkifying
        // text that is not a proper URI. We leave apps actually handling the URI when the user clicks on such a link
        // to deal with this case.
        private val PATTERN = Pattern.compile("$SCHEME:(?://$AUTHORITY)?(?:$PATH)?(?:\\?$QUERY)?(?:#$FRAGMENT)?")
    }
}
