package com.fsck.k9.message.html

import kotlin.math.min

/**
 * Parses http/https/rtsp URIs
 *
 * This class is in parts inspired by OkHttp's
 * [HttpUrl](https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/HttpUrl.java).
 * But much of the parsing parts have been left out.
 */
internal class HttpUriParser : UriParser {
    override fun parseUri(text: CharSequence, startPos: Int): UriMatch? {
        require(startPos in text.indices) { "Invalid 'startPos' value" }

        val matchResult = SCHEME_REGEX.find(text, startPos) ?: return null
        if (matchResult.range.first != startPos) return null

        val skipChar = getSkipChar(text, startPos)
        var currentPos = matchResult.range.last + 1

        // Authority
        val matchedAuthorityEnd = tryMatchAuthority(text, currentPos)
        if (matchedAuthorityEnd == currentPos) return null
        currentPos = matchedAuthorityEnd

        // Path
        if (currentPos < text.length && text[currentPos] == '/') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, "/:@")
        }

        // Query
        if (currentPos < text.length && text[currentPos] == '?') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?")
        }

        // Fragment
        if (currentPos < text.length && text[currentPos] == '#') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?")
        }

        if (text.isEndOfSentence(currentPos - 1)) {
            currentPos--
        }

        if (text[currentPos - 1] == skipChar) {
            currentPos--
        }

        val uri = text.subSequence(startPos, currentPos)
        return UriMatch(startPos, currentPos, uri)
    }

    private fun getSkipChar(text: CharSequence, startPos: Int): Char? {
        if (startPos == 0) return null

        return when (text[startPos - 1]) {
            '(' -> ')'
            else -> null
        }
    }

    private fun tryMatchAuthority(text: CharSequence, startPos: Int): Int {
        var authorityLimit = text.indexOf('/', startPos)
        if (authorityLimit == -1) {
            authorityLimit = text.length
        }
        val authorityStart = tryMatchUserInfo(text, startPos, authorityLimit)

        var authorityEnd = tryMatchDomainName(text, authorityStart)
        if (authorityEnd != authorityStart) return authorityEnd

        authorityEnd = tryMatchIpv4Address(text, authorityStart, true)
        if (authorityEnd != authorityStart) return authorityEnd

        authorityEnd = tryMatchIpv6Address(text, authorityStart)
        if (authorityEnd != authorityStart) return authorityEnd

        return startPos
    }

    private fun tryMatchUserInfo(text: CharSequence, startPos: Int, limit: Int): Int {
        val userInfoEnd = text.indexOf('@', startPos)
        if (userInfoEnd == -1 || userInfoEnd >= limit) return startPos

        return if (matchUnreservedPCTEncodedSubDelimClassesGreedy(text, startPos, ":") != userInfoEnd) {
            // Illegal character in user info
            startPos
        } else {
            userInfoEnd + 1
        }
    }

    private fun tryMatchDomainName(text: CharSequence, startPos: Int): Int {
        val matchResult = DOMAIN_REGEX.find(text, startPos) ?: return startPos
        if (matchResult.range.first != startPos) return startPos

        val portString = matchResult.groupValues[1]
        if (portString.isNotEmpty()) {
            val port = portString.toInt()
            if (port > 65535) return startPos
        }

        return matchResult.range.last + 1
    }

    private fun tryMatchIpv4Address(text: CharSequence, startPos: Int, portAllowed: Boolean): Int {
        val matchResult = IPv4_REGEX.find(text, startPos) ?: return startPos
        if (matchResult.range.first != startPos) return startPos

        for (i in 1..4) {
            val segment = matchResult.groupValues[1].toInt()
            if (segment > 255) return startPos
        }

        if (!portAllowed && matchResult.groupValues[5].isNotEmpty()) return startPos

        val portString = matchResult.groupValues[6]
        if (portString.isNotEmpty()) {
            val port = portString.toInt()
            if (port > 65535) return startPos
        }

        return matchResult.range.last + 1
    }

    private fun tryMatchIpv6Address(text: CharSequence, startPos: Int): Int {
        if (startPos == text.length || text[startPos] != '[') return startPos

        val addressEnd = text.indexOf(']', startPos)
        if (addressEnd == -1) return startPos

        // Actual parsing
        var currentPos = startPos + 1
        var beginSegmentsCount = 0
        var endSegmentsCount = 0

        // Handle :: separator and segments in front of it
        val compressionPos = text.indexOf("::", currentPos)
        val compressionEnabled = compressionPos != -1 && compressionPos < addressEnd
        if (compressionEnabled) {
            while (currentPos < compressionPos) {
                // Check segment separator
                if (beginSegmentsCount > 0) {
                    if (text[currentPos] != ':') return startPos
                    currentPos++
                }

                // Parse segment
                val possibleSegmentEnd = parse16BitHexSegment(text, currentPos, min(currentPos + 4, compressionPos))
                if (possibleSegmentEnd == currentPos) return startPos
                currentPos = possibleSegmentEnd
                beginSegmentsCount++
            }

            currentPos += 2 // Skip :: separator
        }

        // Parse end segments
        while (currentPos < addressEnd && beginSegmentsCount + endSegmentsCount < 8) {
            // Check segment separator
            if (endSegmentsCount > 0) {
                if (text[currentPos] != ':') return startPos
                currentPos++
            }

            // Small look ahead, do not run into IPv4 tail (7 is IPv4 minimum length)
            val nextColon = text.indexOf(':', currentPos)
            if ((nextColon == -1 || nextColon > addressEnd) && addressEnd - currentPos >= 7) break

            // Parse segment
            val possibleSegmentEnd = parse16BitHexSegment(text, currentPos, min(currentPos + 4, addressEnd))
            if (possibleSegmentEnd == currentPos) return startPos
            currentPos = possibleSegmentEnd
            endSegmentsCount++
        }

        // We have 3 valid cases here
        if (currentPos == addressEnd) {
            // 1) No compression and full address, everything fine
            // 2) Compression enabled and whole address parsed, everything fine as well
            if (!compressionEnabled && beginSegmentsCount + endSegmentsCount == 8 ||
                compressionEnabled && beginSegmentsCount + endSegmentsCount < 8
            ) {
                // Only optional port left, skip address bracket
                currentPos++
            } else {
                return startPos
            }
        } else {
            // 3) Still some stuff missing, check for IPv4 as tail necessary
            if (tryMatchIpv4Address(text, currentPos, false) != addressEnd) return startPos
            currentPos = addressEnd + 1
        }

        // Check optional port
        if (currentPos == text.length || text[currentPos] != ':') return currentPos
        currentPos++

        var port = 0
        while (currentPos < text.length) {
            val c = text[currentPos]
            if (c !in '0'..'9') {
                break
            }
            port = port * 10 + (c - '0')
            currentPos++
        }

        return if (port <= 65535) currentPos else startPos
    }

    private fun parse16BitHexSegment(text: CharSequence, startPos: Int, endPos: Int): Int {
        var currentPos = startPos
        while (isHexDigit(text[currentPos]) && currentPos < endPos) {
            currentPos++
        }

        return currentPos
    }

    @Suppress("ConvertToStringTemplate")
    private fun matchUnreservedPCTEncodedSubDelimClassesGreedy(
        text: CharSequence,
        startPos: Int,
        additionalCharacters: String,
    ): Int {
        val allowedCharacters = SUB_DELIM + "-._~" + additionalCharacters
        var shouldBeHex = 0
        var currentPos = startPos
        while (currentPos < text.length) {
            val c = text[currentPos]
            if (isHexDigit(c)) {
                shouldBeHex = (shouldBeHex - 1).coerceAtLeast(0)
            } else if (shouldBeHex == 0) {
                if (c in allowedCharacters) {
                    // Everything ok here :)
                } else if (c == '%') {
                    shouldBeHex = 2
                } else {
                    break
                }
            } else {
                break
            }
            currentPos++
        }

        return currentPos
    }

    private fun isHexDigit(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'
    }

    // This checks if the URL ends in a character that should be ignored because it most likely indicates the end of
    // a sentence rather than being part of the URL
    private fun CharSequence.isEndOfSentence(position: Int): Boolean {
        // We want to keep everything if the URL is wrapped in angle brackets.
        // Example: <https://domain.example/path.>
        if (position < lastIndex && this[position + 1] == '>') return false

        return this[position] in ".?!" && (position == lastIndex || this[position + 1].isWhitespace())
    }

    companion object {
        // This string represent character group sub-delim as described in RFC 3986
        private const val SUB_DELIM = "!$&'()*+,;="
        private val SCHEME_REGEX = "(https?|rtsp)://".toRegex(RegexOption.IGNORE_CASE)
        private val DOMAIN_REGEX =
            "[\\da-z](?:[\\da-z-]*[\\da-z])*(?:\\.[\\da-z](?:[\\da-z-]*[\\da-z])*)*(?::(\\d{0,5}))?"
                .toRegex(RegexOption.IGNORE_CASE)
        private val IPv4_REGEX = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(:(\\d{0,5}))?".toRegex()
    }
}
