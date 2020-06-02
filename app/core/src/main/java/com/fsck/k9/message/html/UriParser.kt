package com.fsck.k9.message.html

internal interface UriParser {
    /**
     * Parse scheme specific URI beginning from given position.
     *
     * @param text String to parse URI from.
     * @param startPos Position where URI starts (first letter of scheme).
     *
     * @return [UriMatch] if a valid URI was found. `null` otherwise.
     */
    fun parseUri(text: CharSequence, startPos: Int): UriMatch?
}
