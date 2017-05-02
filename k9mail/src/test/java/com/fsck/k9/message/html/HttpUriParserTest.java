package com.fsck.k9.message.html;


import org.junit.Test;

import static com.fsck.k9.message.html.UriParserTestHelper.assertLinkOnly;
import static junit.framework.Assert.assertEquals;


public class HttpUriParserTest {
    private final HttpUriParser parser = new HttpUriParser();
    private final StringBuffer outputBuffer = new StringBuffer();


    @Test
    public void emptyUriIgnored() {
        assertLinkIgnored("http://");
    }

    @Test
    public void emptyAuthorityIgnored() {
        assertLinkIgnored("http:///");
    }

    @Test
    public void simpleDomain() {
        assertLinkify("http://www.google.com");
    }

    @Test
    public void simpleDomainWithHttps() {
        assertLinkify("https://www.google.com");
    }

    @Test
    public void simpleRtspUri() {
        assertLinkify("rtsp://example.com/media.mp4");
    }

    @Test
    public void invalidDomainIgnored() {
        assertLinkIgnored("http://-www.google.com");
    }

    @Test
    public void domainWithTrailingSlash() {
        assertLinkify("http://www.google.com/");
    }

    @Test
    public void domainWithUserInfo() {
        assertLinkify("http://test@google.com/");
    }

    @Test
    public void domainWithFullUserInfo() {
        assertLinkify("http://test:secret@google.com/");
    }

    @Test
    public void domainWithoutWww() {
        assertLinkify("http://google.com/");
    }

    @Test
    public void query() {
        assertLinkify("http://google.com/give/me/?q=mode&c=information");
    }

    @Test
    public void fragment() {
        assertLinkify("http://google.com/give/me#only-the-best");
    }

    @Test
    public void queryAndFragment() {
        assertLinkify("http://google.com/give/me/?q=mode&c=information#only-the-best");
    }

    @Test
    public void ipv4Address() {
        assertLinkify("http://127.0.0.1");
    }

    @Test
    public void ipv4AddressWithTrailingSlash() {
        assertLinkify("http://127.0.0.1/");
    }

    @Test
    public void ipv4AddressWithEmptyPort() {
        assertLinkify("http://127.0.0.1:");
    }

    @Test
    public void ipv4AddressWithPort() {
        assertLinkify("http://127.0.0.1:524/");
    }

    @Test
    public void ipv6Address() {
        assertLinkify("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]");
    }

    @Test
    public void ipv6AddressWithPort() {
        assertLinkify("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80");
    }

    @Test
    public void ipv6AddressWithTrailingSlash() {
        assertLinkify("http://[1080:0:0:0:8:800:200C:417A]/");
    }

    @Test
    public void ipv6AddressWithEndCompression() {
        assertLinkify("http://[3ffe:2a00:100:7031::1]");
    }

    @Test
    public void ipv6AddressWithBeginCompression() {
        assertLinkify("http://[1080::8:800:200C:417A]/");
    }

    @Test
    public void ipv6AddressWithCompressionPort() {
        assertLinkify("http://[::FFFF:129.144.52.38]:80/");
    }

    @Test
    public void ipv6AddressWithPrependedCompression() {
        assertLinkify("http://[::192.9.5.5]/");
    }

    @Test
    public void ipv6AddressWithTrailingIp4AndPort() {
        assertLinkify("http://[::192.9.5.5]:80/");
    }

    @Test
    public void ipv6WithoutClosingSquareBracketIgnored() {
        assertLinkIgnored("http://[1080:0:0:0:8:80:200C:417A/");
    }

    @Test
    public void ipv6InvalidClosingSquareBracketIgnored() {
        assertLinkIgnored("http://[1080:0:0:0:8:800:270C:417A/]");
    }

    @Test
    public void domainWithTrailingSpace() {
        String text = "http://google.com/ ";

        int endPos = parser.linkifyUri(text, 0, outputBuffer);

        assertLinkOnly("http://google.com/", outputBuffer);
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void domainWithTrailingNewline() {
        String text = "http://google.com/\n";

        int endPos = parser.linkifyUri(text, 0, outputBuffer);

        assertLinkOnly("http://google.com/", outputBuffer);
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void domainWithTrailingAngleBracket() {
        String text = "<http://google.com/>";

        int endPos = parser.linkifyUri(text, 1, outputBuffer);

        assertLinkOnly("http://google.com/", outputBuffer);
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void uriInMiddleAfterInput() {
        String prefix = "prefix ";
        String uri = "http://google.com/";
        String text = prefix + uri;

        parser.linkifyUri(text, prefix.length(), outputBuffer);

        assertLinkOnly(uri, outputBuffer);
    }

    @Test
    public void uriInMiddleOfInput() {
        String prefix = "prefix ";
        String uri = "http://google.com/";
        String postfix = " postfix";
        String text = prefix + uri + postfix;

        parser.linkifyUri(text, prefix.length(), outputBuffer);

        assertLinkOnly(uri, outputBuffer);
    }


    int linkify(String uri) {
        return parser.linkifyUri(uri, 0, outputBuffer);
    }

    void assertLinkify(String uri) {
        linkify(uri);
        assertLinkOnly(uri, outputBuffer);
    }

    void assertLinkIgnored(String uri) {
        int endPos = linkify(uri);

        assertEquals("", outputBuffer.toString());
        assertEquals(0, endPos);
    }
}
