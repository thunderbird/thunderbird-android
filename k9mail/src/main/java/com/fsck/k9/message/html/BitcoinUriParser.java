package com.fsck.k9.message.html;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


class BitcoinUriParser implements UriParser {
    private static final Pattern BITCOIN_URI_PATTERN =
            Pattern.compile("bitcoin:[1-9a-km-zA-HJ-NP-Z]{27,34}(\\?[a-zA-Z0-9$\\-_.+!*'(),%:@&=]*)?");

    @Nullable
    @Override
    public UriMatch parseUri(@NotNull CharSequence text, int startPos) {
        Matcher matcher = BITCOIN_URI_PATTERN.matcher(text);

        if (!matcher.find(startPos) || matcher.start() != startPos) {
            return null;
        }

        int startIndex = matcher.start();
        int endIndex = matcher.end();
        CharSequence uri = text.subSequence(startIndex, endIndex);
        return new UriMatch(startIndex, endIndex, uri);
    }
}
