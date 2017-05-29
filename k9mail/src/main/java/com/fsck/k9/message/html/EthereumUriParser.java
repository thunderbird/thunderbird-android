package com.fsck.k9.message.html;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ERC-67 URIs
 * https://github.com/ethereum/EIPs/issues/67
 */
class EthereumUriParser implements UriParser {
    private static final Pattern ETHEREUM_URI_PATTERN =
            Pattern.compile("ethereum:0x[0-9a-fA-F]*(\\?[a-zA-Z0-9$\\-_.+!*'(),%:@&=]*)?");

    @Override
    public int linkifyUri(String text, int startPos, StringBuffer outputBuffer) {
        Matcher matcher = ETHEREUM_URI_PATTERN.matcher(text);

        if (!matcher.find(startPos) || matcher.start() != startPos) {
            return startPos;
        }

        String ethereumURI = matcher.group();
        outputBuffer.append("<a href=\"")
                .append(ethereumURI)
                .append("\">")
                .append(ethereumURI)
                .append("</a>");

        return matcher.end();
    }
}
