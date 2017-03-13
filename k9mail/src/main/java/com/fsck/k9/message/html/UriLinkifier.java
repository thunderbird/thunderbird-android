package com.fsck.k9.message.html;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;


/**
 * Allows conversion of link in text to html link.
 */
public class UriLinkifier {
    /**
     * Regular expression pattern to match uri scheme and parsers for supported uris as defined in RFC 3987
     */
    private static final Pattern URI_SCHEME;
    private static final Map<String, UriParser> SUPPORTED_URIS;
    private static final String SCHEME_SEPARATOR = " (";

    static {
        SUPPORTED_URIS = new HashMap<>();
        SUPPORTED_URIS.put("bitcoin:", new BitcoinUriParser());
        UriParser httpParser = new HttpUriParser();
        SUPPORTED_URIS.put("http:", httpParser);
        SUPPORTED_URIS.put("https:", httpParser);
        SUPPORTED_URIS.put("rtsp:", httpParser);

        String allSchemes = TextUtils.join("|", SUPPORTED_URIS.keySet());
        URI_SCHEME = Pattern.compile(allSchemes, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Searches for link-like text in a string and turn it into a link. Append the result to
     * <tt>outputBuffer</tt>. <tt>text</tt> is not modified.
     *
     * @param text
     *         Plain text to be linkified.
     * @param outputBuffer
     *         Buffer to append linked text to.
     */
    public static void linkifyText(final String text, final StringBuffer outputBuffer) {
        int currentPos = 0;
        Matcher matcher = URI_SCHEME.matcher(text);

        while (matcher.find(currentPos)) {
            int startPos = matcher.start();

            String textBeforeMatch = text.substring(currentPos, startPos);
            outputBuffer.append(textBeforeMatch);

            if (!textBeforeMatch.isEmpty() &&
                    !SCHEME_SEPARATOR.contains(textBeforeMatch.substring(textBeforeMatch.length() - 1))) {
                outputBuffer.append(text.charAt(startPos));
                currentPos = startPos + 1;
                continue;
            }

            // Find responsible parser and let it do it's job
            String scheme = matcher.group();
            UriParser parser = SUPPORTED_URIS.get(scheme.toLowerCase());
            int newPos = parser.linkifyUri(text, startPos, outputBuffer);

            // Handle invalid uri, at least advance by one to prevent endless loop
            if (newPos <= startPos) {
                outputBuffer.append(text.charAt(startPos));
                currentPos++;
            } else {
                currentPos = (newPos > currentPos) ? newPos : currentPos + 1;
            }
            if (currentPos >= text.length()) {
                break;
            }
        }

        // Copy rest
        outputBuffer.append(text.substring(currentPos));
    }
}
