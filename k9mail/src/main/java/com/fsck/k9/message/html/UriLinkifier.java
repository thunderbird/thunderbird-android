package com.fsck.k9.message.html;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;


public class UriLinkifier {
    private static final Pattern URI_SCHEME;
    private static final Map<String, UriParser> SUPPORTED_URIS;
    private static final String SCHEME_SEPARATORS = " (\\n";
    private static final String ALLOWED_SEPARATORS_PATTERN = "(?:^|[" + SCHEME_SEPARATORS + "])";

    static {
        SUPPORTED_URIS = new HashMap<>();
        SUPPORTED_URIS.put("ethereum:", new EthereumUriParser());
        SUPPORTED_URIS.put("bitcoin:", new BitcoinUriParser());
        UriParser httpParser = new HttpUriParser();
        SUPPORTED_URIS.put("http:", httpParser);
        SUPPORTED_URIS.put("https:", httpParser);
        SUPPORTED_URIS.put("rtsp:", httpParser);

        String allSchemes = TextUtils.join("|", SUPPORTED_URIS.keySet());
        String pattern = ALLOWED_SEPARATORS_PATTERN + "(" + allSchemes + ")";
        URI_SCHEME = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }


    public static void linkifyText(String text, StringBuffer outputBuffer) {
        int currentPos = 0;
        Matcher matcher = URI_SCHEME.matcher(text);

        while (matcher.find(currentPos)) {
            int startPos = matcher.start(1);

            String textBeforeMatch = text.substring(currentPos, startPos);
            outputBuffer.append(textBeforeMatch);

            String scheme = matcher.group(1).toLowerCase(Locale.US);
            UriParser parser = SUPPORTED_URIS.get(scheme);
            int newPos = parser.linkifyUri(text, startPos, outputBuffer);

            boolean uriWasNotLinkified = newPos <= startPos;
            if (uriWasNotLinkified) {
                outputBuffer.append(text.charAt(startPos));
                currentPos = startPos + 1;
            } else {
                currentPos = (newPos > currentPos) ? newPos : currentPos + 1;
            }

            if (currentPos >= text.length()) {
                break;
            }
        }

        String textAfterLastMatch = text.substring(currentPos);
        outputBuffer.append(textAfterLastMatch);
    }
}
