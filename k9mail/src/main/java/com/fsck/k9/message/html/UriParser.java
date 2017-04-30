package com.fsck.k9.message.html;


public interface UriParser {
    /**
     * Parse and linkify scheme specific URI beginning from given position. The result will be written to given buffer.
     *
     * @param text
     *         String to parse URI from.
     * @param startPos
     *         Position where URI starts (first letter of scheme).
     * @param outputBuffer
     *         Buffer where linkified variant of URI is written to.
     *
     * @return Index where parsed URI ends (first non-URI letter). Should be {@code startPos} or smaller if no valid
     * URI was found.
     */
    int linkifyUri(String text, int startPos, StringBuffer outputBuffer);
}
