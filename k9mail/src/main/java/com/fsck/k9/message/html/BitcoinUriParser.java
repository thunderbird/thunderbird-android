package com.fsck.k9.message.html;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BitcoinUriParser implements UriParser {
    private static final Pattern BITCOIN_URI_PATTERN =
            Pattern.compile("bitcoin:[1-9a-km-zA-HJ-NP-Z]{27,34}(\\?[a-zA-Z0-9$\\-_.+!*'(),%:@&=]*)?");

    @Override
    public int linkifyUri(String text, int startPos, StringBuffer outputBuffer) {
        Matcher matcher = BITCOIN_URI_PATTERN.matcher(text);

        if (!matcher.find(startPos) || matcher.start() != startPos) {
            return startPos;
        }

        String bitcoinUri = matcher.group();
        outputBuffer.append("<a href=\"")
                .append(bitcoinUri)
                .append("\">")
                .append(bitcoinUri)
                .append("</a>");

        return matcher.end();
    }
}
