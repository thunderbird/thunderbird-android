package com.fsck.k9.message.html;


import org.junit.Test;
import static com.fsck.k9.message.html.UriParserTestHelper.assertLinkOnly;
import static org.junit.Assert.assertEquals;


public class EthereumUriParserTest {
    EthereumUriParser parser = new EthereumUriParser();
    StringBuffer outputBuffer = new StringBuffer();


    @Test
    public void basicEthereumUri() throws Exception {
        assertLinkify("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4");
    }

    @Test
    public void ethereumUriWithValue() throws Exception {
        assertLinkify("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42");
    }

    @Test
    public void ethereumUriWithQueryParameters() throws Exception {
        assertLinkify("ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42" +
                "&gas=100000&bytecode=0xa9059cbb0000000000000000000000000000000dead");
    }

    @Test
    public void uriInMiddleOfInput() throws Exception {
        String prefix = "prefix ";
        String uri = "ethereum:0xfdf1210fc262c73d0436236a0e07be419babbbc4?value=42";
        String text = prefix + uri;

        parser.linkifyUri(text, prefix.length(), outputBuffer);

        assertLinkOnly(uri, outputBuffer);
    }

    @Test
    public void invalidScheme() throws Exception {
        assertNotLinkify("ethereMU:0xfdf1210fc262c73d0436236a0e07be419babbbc4");
    }

    @Test
    public void invalidAddress() throws Exception {
        assertNotLinkify("ethereum:[invalid]");
    }

    @Test
    public void invalidEthereumUri_shouldReturnStartingPosition() throws Exception {
        String uri = "ethereum:[invalid]";

        int newPos = linkify(uri);

        assertEquals(0, newPos);
    }

    @Test
    public void invalidEthereumUri_shouldNotWriteToOutputBuffer() throws Exception {
        String uri = "ethereum:[invalid]";

        linkify(uri);

        assertEquals(0, outputBuffer.length());
    }


    int linkify(String uri) {
        return parser.linkifyUri(uri, 0, outputBuffer);
    }

    void assertLinkify(String uri) {
        linkify(uri);
        assertLinkOnly(uri, outputBuffer);
    }

    void assertNotLinkify(String text) {
        int newPos = linkify(text);
        assertEquals(0, newPos);
    }
}
