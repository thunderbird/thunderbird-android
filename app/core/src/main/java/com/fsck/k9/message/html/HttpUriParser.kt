package com.fsck.k9.message.html

import java.util.regex.Pattern
import kotlin.math.max
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
        val schemeMatcher = SCHEME_PATTERN.matcher(text)
        if (!schemeMatcher.find(startPos) || schemeMatcher.start() != startPos) return null

        var currentPos = schemeMatcher.end()

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

        val uri = text.subSequence(startPos, currentPos)
        return UriMatch(startPos, currentPos, uri)
    }

    private fun tryMatchAuthority(text: CharSequence, startPos: Int): Int {
        var authorityLimit = indexOf(text, '/', startPos)
        if (authorityLimit == -1) {
            authorityLimit = text.length
        }
        val authorityStart = tryMatchUserInfo(text, startPos, authorityLimit)

        var authorityEnd = tryMatchDomainName(text, authorityStart)
        if (authorityEnd != authorityStart) {
            return authorityEnd
        }

        authorityEnd = tryMatchIpv4Address(text, authorityStart, true)
        if (authorityEnd != authorityStart) {
            return authorityEnd
        }

        authorityEnd = tryMatchIpv6Address(text, authorityStart)
        return if (authorityEnd != authorityStart) authorityEnd else startPos
    }

    private fun tryMatchUserInfo(text: CharSequence, startPos: Int, limit: Int): Int {
        val userInfoEnd = indexOf(text, '@', startPos)
        return if (userInfoEnd != -1 && userInfoEnd < limit) {
            if (matchUnreservedPCTEncodedSubDelimClassesGreedy(text, startPos, ":") != userInfoEnd) {
                // Illegal character in user info
                startPos
            } else {
                userInfoEnd + 1
            }
        } else {
            startPos
        }
    }

    private fun tryMatchDomainName(text: CharSequence, startPos: Int): Int {
        return try {
            val matcher = DOMAIN_PATTERN.matcher(text)
            if (!matcher.find(startPos) || matcher.start() != startPos) {
                return startPos
            }

            val portString = matcher.group(1)
            if (portString != null && portString.isNotEmpty()) {
                val port = portString.toInt()
                if (port > 65535) {
                    return startPos
                }
            }

            matcher.end()
        } catch (e: IllegalArgumentException) {
            startPos
        }
    }

    private fun tryMatchIpv4Address(text: CharSequence, startPos: Int, portAllowed: Boolean): Int {
        val matcher = IPv4_PATTERN.matcher(text)
        if (!matcher.find(startPos) || matcher.start() != startPos) {
            return startPos
        }

        for (i in 1..4) {
            val segment = matcher.group(1).toInt()
            if (segment > 255) {
                return startPos
            }
        }

        if (!portAllowed && matcher.group(5) != null) {
            return startPos
        }

        val portString = matcher.group(6)
        if (portString != null && portString.isNotEmpty()) {
            val port = portString.toInt()
            if (port > 65535) {
                return startPos
            }
        }

        return matcher.end()
    }

    private fun tryMatchIpv6Address(text: CharSequence, startPos: Int): Int {
        if (startPos == text.length || text[startPos] != '[') {
            return startPos
        }

        val addressEnd = indexOf(text, ']', startPos)
        if (addressEnd == -1) {
            return startPos
        }

        // Actual parsing
        var currentPos = startPos + 1
        var beginSegmentsCount = 0
        var endSegmentsCount = 0

        // Handle :: separator and segments in front of it
        val compressionPos = indexOf(text, "::", currentPos)
        val compressionEnabled = compressionPos != -1 && compressionPos < addressEnd
        if (compressionEnabled) {
            while (currentPos < compressionPos) {
                // Check segment separator
                if (beginSegmentsCount > 0) {
                    if (text[currentPos] != ':') {
                        return startPos
                    } else {
                        ++currentPos
                    }
                }

                // Parse segment
                val possibleSegmentEnd = parse16BitHexSegment(text, currentPos, min(currentPos + 4, compressionPos))
                if (possibleSegmentEnd == currentPos) {
                    return startPos
                }
                currentPos = possibleSegmentEnd
                ++beginSegmentsCount
            }

            currentPos += 2 // Skip :: separator
        }

        // Parse end segments
        while (currentPos < addressEnd && beginSegmentsCount + endSegmentsCount < 8) {
            // Check segment separator
            if (endSegmentsCount > 0) {
                if (text[currentPos] != ':') {
                    return startPos
                } else {
                    ++currentPos
                }
            }

            // Small look ahead, do not run into IPv4 tail (7 is IPv4 minimum length)
            val nextColon = indexOf(text, ':', currentPos)
            if ((nextColon == -1 || nextColon > addressEnd) && addressEnd - currentPos >= 7) {
                break
            }

            // Parse segment
            val possibleSegmentEnd = parse16BitHexSegment(text, currentPos, Math.min(currentPos + 4, addressEnd))
            if (possibleSegmentEnd == currentPos) {
                return startPos
            }
            currentPos = possibleSegmentEnd
            ++endSegmentsCount
        }

        // We have 3 valid cases here
        if (currentPos == addressEnd) {
            // 1) No compression and full address, everything fine
            // 2) Compression enabled and whole address parsed, everything fine as well
            if (!compressionEnabled && beginSegmentsCount + endSegmentsCount == 8 ||
                compressionEnabled && beginSegmentsCount + endSegmentsCount < 8) {
                // Only optional port left, skip address bracket
                ++currentPos
            } else {
                return startPos
            }
        } else {
            // 3) Still some stuff missing, check for IPv4 as tail necessary
            if (tryMatchIpv4Address(text, currentPos, false) != addressEnd) {
                return startPos
            }
            currentPos = addressEnd + 1
        }

        // Check optional port
        if (currentPos == text.length || text[currentPos] != ':') {
            return currentPos
        }
        ++currentPos

        var port = 0
        while (currentPos < text.length) {
            val c = text[currentPos].toInt()
            if (c < '0'.toInt() || c > '9'.toInt()) {
                break
            }
            port = port * 10 + c - '0'.toInt()
            currentPos++
        }
        return if (port <= 65535) currentPos else startPos
    }

    private fun parse16BitHexSegment(text: CharSequence, startPos: Int, endPos: Int): Int {
        var currentPos = startPos
        while (isHexDigit(text[currentPos].toInt()) && currentPos < endPos) {
            ++currentPos
        }

        return currentPos
    }

    private fun matchUnreservedPCTEncodedSubDelimClassesGreedy(
        text: CharSequence, startPos: Int,
        additionalCharacters: String
    ): Int {
        val allowedCharacters = SUB_DELIM + "-._~" + additionalCharacters
        var shouldBeHex = 0
        var currentPos = startPos
        while (currentPos < text.length) {
            val c = text[currentPos].toInt()
            if (isHexDigit(c)) {
                shouldBeHex = max(shouldBeHex - 1, 0)
            } else if (shouldBeHex == 0) {
                if (allowedCharacters.indexOf(c.toChar()) != -1) {
                    // Everything ok here :)
                } else if (c == '%'.toInt()) {
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

    private fun isHexDigit(c: Int): Boolean {
        return c >= 'a'.toInt() && c <= 'z'.toInt() || c >= 'A'.toInt() && c <= 'Z'.toInt() || c >= '0'.toInt() && c <= '9'.toInt()
    }

    private fun indexOf(text: CharSequence, ch: Char, fromIndex: Int): Int {
        var i = fromIndex
        val end = text.length
        while (i < end) {
            if (text[i] == ch) {
                return i
            }
            i++
        }

        return -1
    }

    private fun indexOf(text: CharSequence, str: String, fromIndex: Int): Int {
        val ch = str[0]
        var i = fromIndex
        val end = text.length
        while (i < end) {
            if (text[i] == ch) {
                var found = true
                var j = 1
                val strLen = str.length
                while (j < strLen) {
                    if (text[i + j] != str[j]) {
                        found = false
                        break
                    }
                    j++
                }

                if (found) {
                    return i
                }
            }
            i++
        }

        return -1
    }

    companion object {
        // This string represent character group sub-delim as described in RFC 3986
        private const val SUB_DELIM = "!$&'()*+,;="
        private val SCHEME_PATTERN = Pattern.compile("(https?|rtsp)://", Pattern.CASE_INSENSITIVE)
        private val DOMAIN_PATTERN = Pattern.compile(
            "[\\da-z](?:[\\da-z-]*[\\da-z])*(?:\\.[\\da-z](?:[\\da-z-]*[\\da-z])*)*(?::(\\d{0,5}))?",
            Pattern.CASE_INSENSITIVE
        )
        private val IPv4_PATTERN = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(:(\\d{0,5}))?")
    }
}
