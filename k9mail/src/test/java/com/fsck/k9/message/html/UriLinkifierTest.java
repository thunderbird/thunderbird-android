package com.fsck.k9.message.html;


import com.fsck.k9.K9RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.fsck.k9.message.html.UriParserTestHelper.assertLinkOnly;
import static junit.framework.Assert.assertEquals;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriLinkifierTest {
    private StringBuffer outputBuffer = new StringBuffer();


    @Test
    public void emptyText() {
        String text = "";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals(text, outputBuffer.toString());
    }

    @Test
    public void textWithoutUri_shouldBeCopiedToOutputBuffer() {
        String text = "some text here";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals(text, outputBuffer.toString());
    }

    @Test
    public void simpleUri() {
        String uri = "http://example.org";

        UriLinkifier.linkifyText(uri, outputBuffer);

        assertLinkOnly(uri, outputBuffer);
    }

    @Test
    public void uriPrecededBySpace() {
        String text = " http://example.org";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals(" <a href=\"http://example.org\">http://example.org</a>", outputBuffer.toString());
    }

    @Test
    public void uriPrecededByOpeningParenthesis() {
        String text = "(http://example.org";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals("(<a href=\"http://example.org\">http://example.org</a>", outputBuffer.toString());
    }

    @Test
    public void uriPrecededBySomeText() {
        String uri = "Check out my fantastic URI: http://example.org";

        UriLinkifier.linkifyText(uri, outputBuffer);

        assertEquals("Check out my fantastic URI: <a href=\"http://example.org\">http://example.org</a>",
                outputBuffer.toString());
    }

    @Test
    public void uriWithTrailingText() {
        String uri = "http://example.org/ is the best";

        UriLinkifier.linkifyText(uri, outputBuffer);

        assertEquals("<a href=\"http://example.org/\">http://example.org/</a> is the best", outputBuffer.toString());
    }

    @Test
    public void uriEmbeddedInText() {
        String uri = "prefix http://example.org/ suffix";

        UriLinkifier.linkifyText(uri, outputBuffer);

        assertEquals("prefix <a href=\"http://example.org/\">http://example.org/</a> suffix", outputBuffer.toString());
    }

    @Test
    public void uriWithUppercaseScheme() {
        String uri = "HTTP://example.org/";

        UriLinkifier.linkifyText(uri, outputBuffer);

        assertEquals("<a href=\"HTTP://example.org/\">HTTP://example.org/</a>", outputBuffer.toString());
    }

    @Test
    public void uriNotPrecededByValidSeparator_shouldNotBeLinkified() {
        String text = "myhttp://example.org";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals(text, outputBuffer.toString());
    }

    @Test
    public void uriNotPrecededByValidSeparatorFollowedByValidUri() {
        String text = "myhttp: http://example.org";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals("myhttp: <a href=\"http://example.org\">http://example.org</a>", outputBuffer.toString());
    }

    @Test
    public void schemaMatchWithInvalidUriInMiddleOfTextFollowedByValidUri() {
        String text = "prefix http:42 http://example.org";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals("prefix http:42 <a href=\"http://example.org\">http://example.org</a>", outputBuffer.toString());
    }

    @Test
    public void multipleValidUrisInRow() {
        String text = "prefix http://uri1.example.org some text http://uri2.example.org/path postfix";

        UriLinkifier.linkifyText(text, outputBuffer);

        assertEquals(
                "prefix <a href=\"http://uri1.example.org\">http://uri1.example.org</a> some text " +
                        "<a href=\"http://uri2.example.org/path\">http://uri2.example.org/path</a> postfix",
                outputBuffer.toString());
    }
}
