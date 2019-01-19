package com.fsck.k9.message.html;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class EthereumUriParserTest {
    EthereumUriParser parser = new EthereumUriParser();


    @Test
    public void basicEthereumUri() throws Exception {
        assertValidUri("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4");
    }

    @Test
    public void ethereumUriWithValue() throws Exception {
        assertValidUri("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42");
    }

    @Test
    public void ethereumUriWithQueryParameters() throws Exception {
        assertValidUri("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42" +
                "&gas=100000&bytecode=0xa9059cbb0000000000000000000000000000000dead");
    }

    @Test
    public void uriInMiddleOfInput() throws Exception {
        String prefix = "prefix ";
        String uri = "ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42";
        String text = prefix + uri;

        UriMatch uriMatch = parser.parseUri(text, prefix.length());

        assertUriMatch(uri, uriMatch, prefix.length());
    }

    @Test
    public void invalidScheme() throws Exception {
        assertInvalidUri("ethereMU:0xfdf1210fc262c73d0436236a0e07be419babbbc4");
    }

    @Test
    public void invalidAddress() throws Exception {
        assertInvalidUri("ethereum:[invalid]");
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
