package com.fsck.k9.message.html;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Parses http/https/rtsp URIs
 * <p>
 * This class is in parts inspired by OkHttp's
 * <a href="https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/HttpUrl.java">HttpUrl</a>.
 * But much of the parsing parts have been left out.
 * </p>
 */
class HttpUriParser implements UriParser {
    // This string represent character group sub-delim as described in RFC 3986
    private static final String SUB_DELIM = "!$&'()*+,;=";
    private static final Pattern SCHEME_PATTERN = Pattern.compile("(https?|rtsp)://", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("[\\da-z](?:[\\da-z-]*[\\da-z])*(?:\\.[\\da-z](?:[\\da-z-]*[\\da-z])*)*(?::(\\d{0,5}))?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern IPv4_PATTERN =
            Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(:(\\d{0,5}))?");


    @Nullable
    @Override
    public UriMatch parseUri(@NotNull CharSequence text, int startPos) {
        Matcher schemeMatcher = SCHEME_PATTERN.matcher(text);
        if (!schemeMatcher.find(startPos) || schemeMatcher.start() != startPos) {
            return null;
        }

        int currentPos = schemeMatcher.end();

        // Authority
        int matchedAuthorityEnd = tryMatchAuthority(text, currentPos);
        if (matchedAuthorityEnd == currentPos) {
            return null;
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

        CharSequence uri = text.subSequence(startPos, currentPos);
        return new UriMatch(startPos, currentPos, uri);
    }

    private int tryMatchAuthority(CharSequence text, int startPos) {
        int authorityLimit = indexOf(text, '/', startPos);
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

    private int tryMatchUserInfo(CharSequence text, int startPos, int limit) {
        int userInfoEnd = indexOf(text, '@', startPos);
        if (userInfoEnd != -1 && userInfoEnd < limit) {
            if (matchUnreservedPCTEncodedSubDelimClassesGreedy(text, startPos, ":") != userInfoEnd) {
                // Illegal character in user info
                return startPos;
            }
            return userInfoEnd + 1;
        }
        return startPos;
    }

    private int tryMatchDomainName(CharSequence text, int startPos) {
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

    private int tryMatchIpv4Address(CharSequence text, int startPos, boolean portAllowed) {
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

    private int tryMatchIpv6Address(CharSequence text, int startPos) {
        if (startPos == text.length() || text.charAt(startPos) != '[') {
            return startPos;
        }

        int addressEnd = indexOf(text, ']', startPos);
        if (addressEnd == -1) {
            return startPos;
        }

        // Actual parsing
        int currentPos = startPos + 1;
        int beginSegmentsCount = 0;
        int endSegmentsCount = 0;

        // Handle :: separator and segments in front of it
        int compressionPos = indexOf(text, "::", currentPos);
        boolean compressionEnabled = compressionPos != -1 && compressionPos < addressEnd;
        if (compressionEnabled) {
            while (currentPos < compressionPos) {
                // Check segment separator
                if (beginSegmentsCount > 0) {
                    if (text.charAt(currentPos) != ':') {
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
                if (text.charAt(currentPos) != ':') {
                    return startPos;
                } else {
                    ++currentPos;
                }
            }

            // Small look ahead, do not run into IPv4 tail (7 is IPv4 minimum length)
            int nextColon = indexOf(text, ':', currentPos);
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
        if (currentPos == text.length() || text.charAt(currentPos) != ':') {
            return currentPos;
        }
        ++currentPos;

        int port = 0;
        for (; currentPos < text.length(); currentPos++) {
            int c = text.charAt(currentPos);
            if (c < '0' || c > '9') {
                break;
            }
            port = port * 10 + c - '0';
        }
        return (port <= 65535) ? currentPos : startPos;
    }

    private int parse16BitHexSegment(CharSequence text, int startPos, int endPos) {
        int currentPos = startPos;
        while (isHexDigit(text.charAt(currentPos)) && currentPos < endPos) {
            ++currentPos;
        }

        return currentPos;
    }

    private int matchUnreservedPCTEncodedSubDelimClassesGreedy(CharSequence text, int startPos,
            String additionalCharacters) {
        String allowedCharacters = SUB_DELIM + "-._~" + additionalCharacters;
        int currentPos;
        int shouldBeHex = 0;
        for (currentPos = startPos; currentPos < text.length(); currentPos++) {
            int c = text.charAt(currentPos);

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

    private int indexOf(CharSequence text, char ch, int fromIndex) {
        for (int i = fromIndex, end = text.length(); i < end; i++) {
            if (text.charAt(i) == ch) {
                return i;
            }
        }

        return -1;
    }

    private int indexOf(CharSequence text, String str, int fromIndex) {
        char ch = str.charAt(0);
        for (int i = fromIndex, end = text.length(); i < end; i++) {
            if (text.charAt(i) == ch) {
                boolean found = true;
                for (int j = 1, strLen = str.length(); j < strLen; j++) {
                    if (text.charAt(i + j) != str.charAt(j)) {
                        found = false;
                        break;
                    }
                }

                if (found) {
                    return i;
                }
            }
        }

        return -1;
    }
}
