package com.fsck.k9.message.html;


import com.fsck.k9.K9RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriLinkifierTest {
    @Test
    public void testLinkifyBitcoinAndHttpUri() {
        String text = "bitcoin:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU http://example.com/";

        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals("<a href=\"bitcoin:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU\">" +
                "bitcoin:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU" +
                "</a> " +
                "<a href=\"http://example.com/\">" +
                "http://example.com/" +
                "</a>", outputBuffer.toString());
    }

    @Test
    public void testSimpleHttpUri() {
        String text = "http://www.google.com";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://www.google.com\">http://www.google.com</a>", outputBuffer.toString());
    }

    @Test
    public void testHttpUriWithTrailingSlash() {
        String text = "http://www.google.com/";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://www.google.com/\">http://www.google.com/</a>",
                outputBuffer.toString());
    }

    @Test
    public void testHttpUriWithoutWWW() {
        String text = "http://google.com/";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a>", outputBuffer.toString());
    }

    @Test
    public void testHttpUriWithTrailingSpace() {
        String text = "http://google.com/ ";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a> ", outputBuffer.toString());
    }

    @Test
    public void testHttpUriWithTrailingSpaceNewline() {
        String text = "http://google.com/ \n";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a> \n",
                outputBuffer.toString());
    }

    @Test
    public void testHttpUriWithTrailingNewline() {
        String text = "http://google.com/\n";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("<a href=\"http://google.com/\">http://google.com/</a>\n", outputBuffer.toString());
    }

    @Test
    public void testIgnorePartialHttpUriScheme() {
        String text = "myhttp://example.org";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals(text, outputBuffer.toString());
    }

    @Test
    public void testPartialHttpUriSchemeWithSeparator() {
        String text = "(http://example.org";
        StringBuffer outputBuffer = new StringBuffer();
        UriLinkifier.linkifyText(text, outputBuffer);
        assertEquals("(<a href=\"http://example.org\">http://example.org</a>", outputBuffer.toString());
    }
}
