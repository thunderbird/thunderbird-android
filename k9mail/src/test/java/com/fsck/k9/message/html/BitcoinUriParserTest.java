package com.fsck.k9.message.html;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class BitcoinUriParserTest {
    BitcoinUriParser parser = new BitcoinUriParser();


    @Test
    public void basicBitcoinUri() throws Exception {
        assertValidUri("bitcoin:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU");
    }

    @Test
    public void bitcoinUriWithAmount() throws Exception {
        assertValidUri("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2");
    }

    @Test
    public void bitcoinUriWithQueryParameters() throws Exception {
        assertValidUri("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2" +
                "&message=Payment&label=Satoshi&extra=other-param");
    }

    @Test
    public void uriInMiddleOfInput() throws Exception {
        String prefix = "prefix ";
        String uri = "bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2";
        String text = prefix + uri;

        UriMatch uriMatch = parser.parseUri(text, prefix.length());

        assertUriMatch(uri, uriMatch, prefix.length());
    }

    @Test
    public void invalidScheme() throws Exception {
        assertInvalidUri("bitcion:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU");
    }

    @Test
    public void invalidAddress() throws Exception {
        assertInvalidUri("bitcoin:[invalid]");
    }


    private void assertValidUri(String uri) {
        UriMatch uriMatch = parser.parseUri(uri, 0);
        assertUriMatch(uri, uriMatch);
    }

    private void assertUriMatch(String uri, UriMatch uriMatch) {
        assertUriMatch(uri, uriMatch, 0);
    }

    private void assertUriMatch(String uri, UriMatch uriMatch, int offset) {
        assertNotNull(uriMatch);
        assertEquals(offset, uriMatch.getStartIndex());
        assertEquals(uri.length() + offset, uriMatch.getEndIndex());
        assertEquals(uri, uriMatch.getUri().toString());
    }

    private void assertInvalidUri(String text) {
        UriMatch uriMatch = parser.parseUri(text, 0);
        assertNull(uriMatch);
    }
}
