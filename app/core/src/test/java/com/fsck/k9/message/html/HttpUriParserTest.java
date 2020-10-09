package com.fsck.k9.message.html;


import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class HttpUriParserTest {
    private final HttpUriParser parser = new HttpUriParser();


    @Test
    public void emptyUriIgnored() {
        assertInvalidUri("http://");
    }

    @Test
    public void emptyAuthorityIgnored() {
        assertInvalidUri("http:///");
    }

    @Test
    public void simpleDomain() {
        assertValidUri("http://www.google.com");
    }

    @Test
    public void simpleDomainWithHttps() {
        assertValidUri("https://www.google.com");
    }

    @Test
    public void simpleRtspUri() {
        assertValidUri("rtsp://example.com/media.mp4");
    }

    @Test
    public void invalidDomainIgnored() {
        assertInvalidUri("http://-www.google.com");
    }

    @Test
    public void domainWithTrailingSlash() {
        assertValidUri("http://www.google.com/");
    }

    @Test
    public void domainWithUserInfo() {
        assertValidUri("http://test@google.com/");
    }

    @Test
    public void domainWithFullUserInfo() {
        assertValidUri("http://test:secret@google.com/");
    }

    @Test
    public void domainWithoutWww() {
        assertValidUri("http://google.com/");
    }

    @Test
    public void query() {
        assertValidUri("http://google.com/give/me/?q=mode&c=information");
    }

    @Test
    public void fragment() {
        assertValidUri("http://google.com/give/me#only-the-best");
    }

    @Test
    public void queryAndFragment() {
        assertValidUri("http://google.com/give/me/?q=mode&c=information#only-the-best");
    }

    @Test
    public void ipv4Address() {
        assertValidUri("http://127.0.0.1");
    }

    @Test
    public void ipv4AddressWithTrailingSlash() {
        assertValidUri("http://127.0.0.1/");
    }

    @Test
    public void ipv4AddressWithEmptyPort() {
        assertValidUri("http://127.0.0.1:");
    }

    @Test
    public void ipv4AddressWithPort() {
        assertValidUri("http://127.0.0.1:524/");
    }

    @Test
    public void ipv6Address() {
        assertValidUri("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]");
    }

    @Test
    public void ipv6AddressWithPort() {
        assertValidUri("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80");
    }

    @Test
    public void ipv6AddressWithTrailingSlash() {
        assertValidUri("http://[1080:0:0:0:8:800:200C:417A]/");
    }

    @Test
    public void ipv6AddressWithEndCompression() {
        assertValidUri("http://[3ffe:2a00:100:7031::1]");
    }

    @Test
    public void ipv6AddressWithBeginCompression() {
        assertValidUri("http://[1080::8:800:200C:417A]/");
    }

    @Test
    public void ipv6AddressWithCompressionPort() {
        assertValidUri("http://[::FFFF:129.144.52.38]:80/");
    }

    @Test
    public void ipv6AddressWithPrependedCompression() {
        assertValidUri("http://[::192.9.5.5]/");
    }

    @Test
    public void ipv6AddressWithTrailingIp4AndPort() {
        assertValidUri("http://[::192.9.5.5]:80/");
    }

    @Test
    public void ipv6WithoutClosingSquareBracketIgnored() {
        assertInvalidUri("http://[1080:0:0:0:8:80:200C:417A/");
    }

    @Test
    public void ipv6InvalidClosingSquareBracketIgnored() {
        assertInvalidUri("http://[1080:0:0:0:8:800:270C:417A/]");
    }

    @Test
    public void domainWithTrailingSpace() {
        String text = "http://google.com/ ";

        UriMatch uriMatch = parser.parseUri(text, 0);

        assertUriMatch("http://google.com/", uriMatch);
    }

    @Test
    public void domainWithTrailingNewline() {
        String text = "http://google.com/\n";

        UriMatch uriMatch = parser.parseUri(text, 0);

        assertUriMatch("http://google.com/", uriMatch);
    }

    @Test
    public void domainWithTrailingAngleBracket() {
        String text = "<http://google.com/>";

        UriMatch uriMatch = parser.parseUri(text, 1);

        assertUriMatch("http://google.com/", uriMatch, 1);
    }

    @Test
    public void uriInMiddleAfterInput() {
        String prefix = "prefix ";
        String uri = "http://google.com/";
        String text = prefix + uri;

        UriMatch uriMatch = parser.parseUri(text, prefix.length());

        assertUriMatch("http://google.com/", uriMatch, prefix.length());
    }

    @Test
    public void uriInMiddleOfInput() {
        String prefix = "prefix ";
        String uri = "http://google.com/";
        String postfix = " postfix";
        String text = prefix + uri + postfix;

        UriMatch uriMatch = parser.parseUri(text, prefix.length());

        assertUriMatch("http://google.com/", uriMatch, prefix.length());
    }

    @Test
    public void uriWrappedInParentheses() {
        String input = "(https://domain.example/)";

        UriMatch uriMatch = parser.parseUri(input, 1);

        assertUriMatch("https://domain.example/", uriMatch, 1);
    }

    @Test
    public void uriContainingParentheses() {
        String input = "https://domain.example/(parentheses)";

        UriMatch uriMatch = parser.parseUri(input, 0);

        assertUriMatch("https://domain.example/(parentheses)", uriMatch, 0);
    }

    @Test
    public void uriContainingParenthesesWrappedInParentheses() {
        String input = "(https://domain.example/(parentheses))";

        UriMatch uriMatch = parser.parseUri(input, 1);

        assertUriMatch("https://domain.example/(parentheses)", uriMatch, 1);
    }

    @Test
    public void uriEndingInDotAtEndOfText() {
        String input = "URL: https://domain.example/path.";

        UriMatch uriMatch = parser.parseUri(input, 5);

        assertUriMatch("https://domain.example/path", uriMatch, 5);
    }


    @Test
    public void uriEndingInDotWithAdditionalText() {
        String input = "URL: https://domain.example/path. Some other text";

        UriMatch uriMatch = parser.parseUri(input, 5);

        assertUriMatch("https://domain.example/path", uriMatch, 5);
    }

    @Test
    public void uriWrappedInAngleBracketsEndingInDot() {
        String input = "URL: <https://domain.example/path.>";

        UriMatch uriMatch = parser.parseUri(input, 6);

        assertUriMatch("https://domain.example/path.", uriMatch, 6);
    }

    @Test
    public void uriWrappedInParenthesesEndingInDot() {
        String input = "URL: (https://domain.example/path.)";

        UriMatch uriMatch = parser.parseUri(input, 6);

        assertUriMatch("https://domain.example/path.", uriMatch, 6);
    }

    @Test
    public void uriWrappedInParenthesesFollowedByADot() {
        String input = "URL: (https://domain.example/path).";

        UriMatch uriMatch = parser.parseUri(input, 6);

        assertUriMatch("https://domain.example/path", uriMatch, 6);
    }

    @Test
    public void uriWrappedInParenthesesFollowedByADotAndSomeOtherText() {
        String input = "URL: (https://domain.example/path). Some other text";

        UriMatch uriMatch = parser.parseUri(input, 6);

        assertUriMatch("https://domain.example/path", uriMatch, 6);
    }

    @Test
    public void uriWrappedInParenthesesFollowedByAQuestionMarkAndSomeOtherText() {
        String input = "URL: (https://domain.example/path)? Some other text";

        UriMatch uriMatch = parser.parseUri(input, 6);

        assertUriMatch("https://domain.example/path", uriMatch, 6);
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
        Assert.assertEquals(offset, uriMatch.getStartIndex());
        Assert.assertEquals(uri.length() + offset, uriMatch.getEndIndex());
        Assert.assertEquals(uri, uriMatch.getUri().toString());
    }

    private void assertInvalidUri(String uri) {
        UriMatch uriMatch = parser.parseUri(uri, 0);
        assertNull(uriMatch);
    }
}
