package com.fsck.k9.message.html;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses and "linkifies" http links.
 * <p>
 * This class is in parts inspired by OkHttp's
 * <a href="https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/HttpUrl.java">HttpUrl</a>.
 * But much of the parsing parts have been left out.
 * </p>
 */
class HttpUriParser implements UriParser {
    // This string represent character group sub-delim as described in RFC 3986
    private static final String SUB_DELIM = "!$&'()*+,;=";
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("[\\da-z](?:[\\da-z-]*[\\da-z])*(?:\\.[\\da-z](?:[\\da-z-]*[\\da-z])*)*(?::(\\d{0,5}))?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern IPv4_PATTERN =
            Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(:(\\d{0,5}))?");


    @Override
    public int linkifyUri(String text, int startPos, StringBuffer outputBuffer) {
        int currentPos = startPos;

        // Scheme
        String shortScheme = text.substring(currentPos, Math.min(currentPos + 7, text.length()));
        String longScheme = text.substring(currentPos, Math.min(currentPos + 8, text.length()));
        if (longScheme.equalsIgnoreCase("https://")) {
            currentPos += "https://".length();
        } else if (shortScheme.equalsIgnoreCase("http://")) {
            currentPos += "http://".length();
        } else if (shortScheme.equalsIgnoreCase("rtsp://")) {
            currentPos += "rtsp://".length();
        } else {
            return startPos;
        }

        // Authority
        int matchedAuthorityEnd = tryMatchAuthority(text, currentPos);
        if (matchedAuthorityEnd == currentPos) {
            return startPos;
        }
        currentPos = matchedAuthorityEnd;

        // Path
        if (currentPos < text.length() && text.charAt(currentPos) == '/') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, "/:@");
        }

        // Query
        if (currentPos < text.length() && text.charAt(currentPos) == '?') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?");
        }

        // Fragment
        if (currentPos < text.length() && text.charAt(currentPos) == '#') {
            currentPos = matchUnreservedPCTEncodedSubDelimClassesGreedy(text, currentPos + 1, ":@/?");
        }

        String httpUri = text.substring(startPos, currentPos);
        outputBuffer.append("<a href=\"")
                .append(httpUri)
                .append("\">")
                .append(httpUri)
                .append("</a>");

        return currentPos;
    }

    private int tryMatchAuthority(String text, int startPos) {
        int authorityLimit = text.indexOf('/', startPos);
        if (authorityLimit == -1) {
            authorityLimit = text.length();
        }
        int authorityStart = tryMatchUserInfo(text, startPos, authorityLimit);

        int authorityEnd = tryMatchDomainName(text, authorityStart);
        if (authorityEnd != authorityStart) {
            return authorityEnd;
        }

        authorityEnd = tryMatchIpv4Address(text, authorityStart, true);
        if (authorityEnd != authorityStart) {
            return authorityEnd;
        }

        authorityEnd = tryMatchIpv6Address(text, authorityStart);
        if (authorityEnd != authorityStart) {
            return authorityEnd;
        }

        return startPos;
    }

    private int tryMatchUserInfo(String text, int startPos, int limit) {
        int userInfoEnd = text.indexOf('@', startPos);
        if (userInfoEnd != -1 && userInfoEnd < limit) {
            if (matchUnreservedPCTEncodedSubDelimClassesGreedy(text, startPos, ":") != userInfoEnd) {
                // Illegal character in user info
                return startPos;
            }
            return userInfoEnd + 1;
        }
        return startPos;
    }

    private int tryMatchDomainName(String text, int startPos) {
        try {
            Matcher matcher = DOMAIN_PATTERN.matcher(text);
            if (!matcher.find(startPos) || matcher.start() != startPos) {
                return startPos;
            }

            String portString = matcher.group(1);
            if (portString != null && !portString.isEmpty()) {
                int port = Integer.parseInt(portString);
                if (port > 65535) {
                    return startPos;
                }
            }

            return matcher.end();
        } catch (IllegalArgumentException e) {
            return startPos;
        }
    }

    private int tryMatchIpv4Address(String text, int startPos, boolean portAllowed) {
        Matcher matcher = IPv4_PATTERN.matcher(text);
        if (!matcher.find(startPos) || matcher.start() != startPos) {
            return startPos;
        }

        for (int i = 1; i <= 4; i++) {
            int segment = Integer.parseInt(matcher.group(1));
            if (segment > 255) {
                return startPos;
            }
        }

        if (!portAllowed && matcher.group(5) != null) {
            return startPos;
        }

        String portString = matcher.group(6);
        if (portString != null && !portString.isEmpty()) {
            int port = Integer.parseInt(portString);
            if (port > 65535) {
                return startPos;
            }
        }

        return matcher.end();
    }

    private int tryMatchIpv6Address(String text, int startPos) {
        if (startPos == text.length() || text.codePointAt(startPos) != '[') {
            return startPos;
        }

        int addressEnd = text.indexOf(']');
        if (addressEnd == -1) {
            return startPos;
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
                        return startPos;
                    } else {
                        ++currentPos;
                    }
                }

                // Parse segment
                int possibleSegmentEnd =
                        parse16BitHexSegment(text, currentPos, Math.min(currentPos + 4, compressionPos));
                if (possibleSegmentEnd == currentPos) {
                    return startPos;
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
                    return startPos;
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
                return startPos;
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
                return startPos;
            }
        } else {
            // 3) Still some stuff missing, check for IPv4 as tail necessary
            if (tryMatchIpv4Address(text, currentPos, false) != addressEnd) {
                return startPos;
            }
            currentPos = addressEnd + 1;
        }

        // Check optional port
        if (currentPos == text.length() || text.codePointAt(currentPos) != ':') {
            return currentPos;
        }
        ++currentPos;

        int port = 0;
        for (; currentPos < text.length(); currentPos++) {
            int c = text.codePointAt(currentPos);
            if (c < '0' || c > '9') {
                break;
            }
            port = port * 10 + c - '0';
        }
        return (port <= 65535) ? currentPos : startPos;
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
