package com.fsck.k9.message.html;


import com.fsck.k9.K9RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HttpUriParserTest {
    private HttpUriParser parser;
    private StringBuffer outputBuffer;

    @Before
    public void setUp() {
        parser = new HttpUriParser();
        outputBuffer = new StringBuffer();
    }

    @Test
    public void testSimpleDomain() {
        String text = "http://www.google.com";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainWithTrailingSlash() {
        String text = "http://www.google.com/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainWithoutWWW() {
        String text = "http://google.com/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainWithTrailingSpace() {
        String text = "http://google.com/ ";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a>", outputBuffer.toString());
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void testDomainWithTrailingSpaceNewline() {
        String text = "http://google.com/ \n";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a>", outputBuffer.toString());
        assertEquals(text.length() - 2, endPos);
    }

    @Test
    public void testDomainWithTrailingNewline() {
        String text = "http://google.com/\n";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a>", outputBuffer.toString());
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void testDomainsWithQueryAndFragment() {
        String text = "http://google.com/give/me/?q=mode&c=information#only-the-best";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(
                "<a href=\"http://google.com/give/me/?q=mode&c=information#only-the-best\">http://google.com/give/me/?q=mode&c=information#only-the-best</a>",
                outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainsWithQuery() {
        String text = "http://google.com/give/me/?q=mode&c=information";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(
                "<a href=\"http://google.com/give/me/?q=mode&c=information\">http://google.com/give/me/?q=mode&c=information</a>",
                outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainsWithFragment() {
        String text = "http://google.com/give/me#only-the-best";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(
                "<a href=\"http://google.com/give/me#only-the-best\">http://google.com/give/me#only-the-best</a>",
                outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testDomainsWithQueryAndFragmentWithoutWWWW() {
        String text = "http://google.com/give/me/?q=mode+c=information#only-the-best\n";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(
                "<a href=\"http://google.com/give/me/?q=mode+c=information#only-the-best\">http://google.com/give/me/?q=mode+c=information#only-the-best</a>",
                outputBuffer.toString());
        assertEquals(text.length() - 1, endPos);
    }

    @Test
    public void testIpv4Address() {
        String text = "http://127.0.0.1";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv4AddressWithTrailingSlash() {
        String text = "http://127.0.0.1/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv4AddressWithEmptyPort() {
        String text = "http://127.0.0.1:";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv4AddressWithPort() {
        String text = "http://127.0.0.1:524/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6Address() {
        String text = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithPort() {
        String text = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressShort() {
        String text = "http://[1080:0:0:0:8:800:200C:417A]/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithEndCompression() {
        String text = "http://[3ffe:2a00:100:7031::1]";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithBeginCompression() {
        String text = "http://[1080::8:800:200C:417A]/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithPrependedCompression() {
        String text = "http://[::192.9.5.5]/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithCompressionPort() {
        String text = "http://[::FFFF:129.144.52.38]:80/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithTrailingIp4() {
        String text = "http://[::192.9.5.5]/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }

    @Test
    public void testIpv6AddressWithTrailingIp4AndPort() {
        String text = "http://[::192.9.5.5]:80/";
        int endPos = parser.linkifyUri(text, 0, outputBuffer);
        assertEquals(String.format("<a href=\"%1$s\">%1$s</a>", text), outputBuffer.toString());
        assertEquals(text.length(), endPos);
    }
}
