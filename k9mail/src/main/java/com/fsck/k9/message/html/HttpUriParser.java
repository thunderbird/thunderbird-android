package com.fsck.k9.message.html;


import java.net.IDN;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses and "linkifies" http links.
 * <p>
 * This class is in parts inspired by OkHttp's HttpUrl (https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/HttpUrl.java),s
 * but leaving out much of the parsing part.
 */
class HttpUriParser implements UriParser {
    // This string represent character group sub-delim as described in RFC 3986
    private static final String SUB_DELIM = "!$&'()*+,;=";
    private static final Pattern IPv4_PATTERN =
            Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(:(\\d{0,5}))?");

    @Override
    public int linkifyUri(String text, int startPos, StringBuffer outputBuffer) {
        int currentPos = startPos;

        // Test scheme
        String shortScheme = text.substring(currentPos, Math.min(currentPos + 7, text.length()));
        String longScheme = text.substring(currentPos, Math.min(currentPos + 8, text.length()));
        if (shortScheme.equalsIgnoreCase("https://")) {
            currentPos += "https://".length();
        } else if (shortScheme.equalsIgnoreCase("http://")) {
            currentPos += "http://".length();
        } else if (longScheme.equalsIgnoreCase("rtsp://")) {
            currentPos += "rtsp://".length();
        } else {
            // Unsupported scheme
            return startPos;
        }

        // Test authority
        int authorityEnd = text.indexOf('/', currentPos);
        if (authorityEnd == -1) {
            authorityEnd = text.length();
        }

        // Authority: Take a look at user info if available
        currentPos = matchUserInfoIfAvailable(text, currentPos, authorityEnd);

        // Authority: Take a look at host
        if (!tryMatchDomainName(text, currentPos, authorityEnd) &&
                !tryMatchIpv4Address(text, currentPos, authorityEnd, true) &&
                !tryMatchIpv6Address(text, currentPos, authorityEnd)) {
            return startPos;
        }
        currentPos = authorityEnd;

        // Test path
        if (currentPos < text.length() && text.charAt(currentPos) == '/') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, "/:@");
        }

        // Test for query
        if (currentPos < text.length() && text.charAt(currentPos) == '?') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?");
        }

        // Test for fragment.
        if (currentPos < text.length() && text.charAt(currentPos) == '#') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?");
        }

        // Final link generation
        String linkifiedUri = String.format("<a href=\"%1$s\">%1$s</a>", text.substring(startPos, currentPos));
        outputBuffer.append(linkifiedUri);

        return currentPos;
    }

    private int matchUserInfoIfAvailable(String text, int startPos, int authorityEnd) {
        int userInfoEnd = text.indexOf('@', startPos);
        if (userInfoEnd != -1 && userInfoEnd < authorityEnd) {
            if (matchUnreservedPCTEncodedSubDelimClassesGreedy(text, startPos, ":") != userInfoEnd) {
                // Illegal character in user info
                return startPos;
            }
            return userInfoEnd + 1;
        }
        return startPos;
    }

    private boolean tryMatchDomainName(String text, int startPos, int authorityEnd) {
        // Partly from OkHttp's HttpUrl (https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/HttpUrl.java)
        try {
            // Check for port
            int portPos = text.indexOf(':', startPos);
            boolean hasPort = portPos != -1 && portPos < authorityEnd;
            if (hasPort) {
                int port = 0;
                for (int i = portPos + 1; i < authorityEnd; i++) {
                    int c = text.codePointAt(i);
                    if (c < '0' || c > '9') {
                        return false;
                    }
                    port = port * 10 + c - '0';
                }
                if (port > 65535) {
                    return false;
                }
            }

            // Check actual domain
            String result = IDN.toASCII(text.substring(startPos, authorityEnd)).toLowerCase(Locale.US);
            if (result.isEmpty()) {
                return false;
            }

            // Confirm that the IDN ToASCII result doesn't contain any illegal characters.
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                // The WHATWG Host parsing rules accepts some character codes which are invalid by
                // definition for OkHttp's host header checks (and the WHATWG Host syntax definition). Here
                // we rule out characters that would cause problems in host headers.
                if (c <= '\u001f' || c >= '\u007f') {
                    return false;
                }
                // Check for the characters mentioned in the WHATWG Host parsing spec:
                // U+0000, U+0009, U+000A, U+000D, U+0020, "#", "%", "/", ":", "?", "@", "[", "\", and "]"
                // (excluding the characters covered above).
                if (" #%/:?@[\\]".indexOf(c) != -1) {
                    return false;
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean tryMatchIpv4Address(String text, int startPos, int authorityEnd, boolean portAllowed) {
        Matcher matcher = IPv4_PATTERN.matcher(text.subSequence(startPos, authorityEnd));
        if (!matcher.matches()) {
            return false;
        }

        // Validate segments
        for (int i = 1; i <= 4; i++) {
            int segment = Integer.parseInt(matcher.group(1));
            if (segment > 255) {
                return false;
            }
        }

        // Make sure port does not exist if missing
        if (!portAllowed && matcher.group(5) != null) {
            return false;
        }

        // Validate optional port
        String portString = matcher.group(6);
        if (portString != null && !portString.isEmpty()) {
            int port = Integer.parseInt(portString);
            if (port > 65535) {
                return false;
            }
        }

        return true;
    }

    private boolean tryMatchIpv6Address(String text, int startPos, int authorityEnd) {
        // General validation
        if (text.codePointAt(startPos) != '[') {
            return false;
        }

        int addressEnd = text.indexOf(']');
        if (addressEnd == -1 || addressEnd >= authorityEnd) {
            return false;
        }

        // Actual parsing
        int currentPos = startPos + 1;
        int beginSegmentsCount = 0;
        int endSegmentsCount = 0;

        // Handle :: separator and segments in front of it
        int compressionPos = text.indexOf("::");
        boolean compressionEnabled = compressionPos != -1 && compressionPos < addressEnd;
        if (compressionEnabled) {
            while (currentPos < compressionPos) {
                // Check segment separator
                if (beginSegmentsCount > 0) {
                    if (text.codePointAt(currentPos) != ':') {
                        return false;
                    } else {
                        ++currentPos;
                    }
                }

                // Parse segment
                int possibleSegmentEnd =
                        parse16BitHexSegment(text, currentPos, Math.min(currentPos + 4, compressionPos));
                if (possibleSegmentEnd == currentPos) {
                    return false;
                }
                currentPos = possibleSegmentEnd;
                ++beginSegmentsCount;
            }

            currentPos += 2; // Skip :: separator
        }

        // Parse end segments
        while (currentPos < addressEnd && (beginSegmentsCount + endSegmentsCount) < 8) {
            // Check segment separator
            if (endSegmentsCount > 0) {
                if (text.codePointAt(currentPos) != ':') {
                    return false;
                } else {
                    ++currentPos;
                }
            }

            // Small look ahead, do not run into IPv4 tail (7 is IPv4 minimum length)
            int nextColon = text.indexOf(':', currentPos);
            if ((nextColon == -1 || nextColon > addressEnd) && (addressEnd - currentPos) >= 7) {
                break;
            }

            // Parse segment
            int possibleSegmentEnd = parse16BitHexSegment(text, currentPos, Math.min(currentPos + 4, addressEnd));
            if (possibleSegmentEnd == currentPos) {
                return false;
            }
            currentPos = possibleSegmentEnd;
            ++endSegmentsCount;
        }

        // We have 3 valid cases here
        if (currentPos == addressEnd) {
            // 1) No compression and full address, everything fine
            // 2) Compression enabled and whole address parsed, everything fine as well
            if ((!compressionEnabled && beginSegmentsCount + endSegmentsCount == 8) ||
                    (compressionEnabled && beginSegmentsCount + endSegmentsCount < 8)) {
                // Only optional port left, skip address bracket
                ++currentPos;
            } else {
                return false;
            }
        } else {
            // 3) Still some stuff missing, check for IPv4 as tail necessary
            if (!tryMatchIpv4Address(text, currentPos, addressEnd, false)) {
                return false;
            }
            currentPos = addressEnd + 1;
        }

        // Check optional port
        if (currentPos == authorityEnd) {
            return true;
        }
        if (text.codePointAt(currentPos) != ':' || currentPos + 1 == authorityEnd) {
            return false;
        }
        ++currentPos;

        int port = 0;
        for (int i = currentPos; i < authorityEnd; i++) {
            int c = text.codePointAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            port = port * 10 + c - '0';
        }
        return port <= 65535;
    }

    private int parse16BitHexSegment(String text, int startPos, int endPos) {
        int currentPos = startPos;
        while (isHexDigit(text.codePointAt(currentPos)) && currentPos < endPos) {
            ++currentPos;
        }

        return currentPos;
    }

    private int matchUnreservedPCTEncodedSubDelimClassesGreedy(String text, int startPos, String additionalCharacters) {
        String allowedCharacters = SUB_DELIM + "-._~" + additionalCharacters;
        int currentPos;
        int shouldBeHex = 0;
        for (currentPos = startPos; currentPos < text.length(); currentPos++) {
            int c = text.codePointAt(currentPos);

            if (isHexDigit(c)) {
                shouldBeHex = Math.max(shouldBeHex - 1, 0);
            } else if (shouldBeHex == 0) {
                if (allowedCharacters.indexOf(c) != -1) {
                    // Everything ok here :)
                } else if (c == '%') {
                    shouldBeHex = 2;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return currentPos;
    }

    private boolean isHexDigit(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
