package com.fsck.k9.message.html;

/**
 * General framework to handle uris when parsing. Allows different handling depending on the scheme identifier.
 */
public interface UriParser {
    /**
     * Parse and linkify scheme specific uri beginning from given position. The result will be written to given buffer.
     * @param text String to parse uri from.
     * @param startPos Position where uri starts (first letter of scheme).
     * @param outputBuffer Buffer where linkified variant of uri is written to.
     * @return Index where parsed uri ends (first non-uri letter). Should be startPos or smaller if no valid uri was found.
     */
    int linkifyUri(String text, int startPos, StringBuffer outputBuffer);
}
