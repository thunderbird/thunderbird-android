package com.fsck.k9.message.extractors;


import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.fsck.k9.message.MessageCreationHelper.createTextPart;
import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
public class PreviewTextExtractorTest {
    private PreviewTextExtractor previewTextExtractor;


    @Before
    public void setUp() throws Exception {
        previewTextExtractor = new PreviewTextExtractor();
    }

    @Test(expected = PreviewExtractionException.class)
    public void extractPreview_withEmptyBody_shouldThrow() throws Exception {
        Part part = new MimeBodyPart(null, "text/plain");

        previewTextExtractor.extractPreview(part);
    }

    @Test
    public void extractPreview_withSimpleTextPlain() throws Exception {
        String text = "The quick brown fox jumps over the lazy dog";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals(text, preview);
    }

    @Test
    public void extractPreview_withSimpleTextHtml() throws Exception {
        String text = "<b>The quick brown fox jumps over the lazy dog</b>";
        Part part = createTextPart("text/html", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("The quick brown fox jumps over the lazy dog", preview);
    }

    @Test
    public void extractPreview_withLongTextPlain() throws Exception {
        String text = "" +
                "10--------20--------30--------40--------50--------" +
                "60--------70--------80--------90--------100-------" +
                "110-------120-------130-------140-------150-------" +
                "160-------170-------180-------190-------200-------" +
                "210-------220-------230-------240-------250-------" +
                "260-------270-------280-------290-------300-------" +
                "310-------320-------330-------340-------350-------" +
                "360-------370-------380-------390-------400-------" +
                "410-------420-------430-------440-------450-------" +
                "460-------470-------480-------490-------500-------" +
                "510-------520-------";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals(text.substring(0, 511) + "…", preview);
    }

    @Test
    public void extractPreview_shouldStripSignature() throws Exception {
        String text = "" +
                "Some text\r\n" +
                "-- \r\n" +
                "Signature";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("Some text", preview);
    }

    @Test
    public void extractPreview_shouldStripHorizontalLine() throws Exception {
        String text = "" +
                "line 1\r\n" +
                "----\r\n" +
                "line 2";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("line 1 line 2", preview);
    }

    @Test
    public void extractPreview_shouldStripQuoteHeaderAndQuotedText() throws Exception {
        String text = "" +
                "some text\r\n" +
                "On 01/02/03 someone wrote\r\n" +
                "> some quoted text\r\n" +
                "# some other quoted text\r\n";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("some text", preview);
    }

    @Test
    public void extractPreview_shouldStripGenericQuoteHeader() throws Exception {
        String text = "" +
                "Am 13.12.2015 um 23:42 schrieb Hans:\r\n" +
                "> hallo\r\n" +
                "hi there\r\n";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("hi there", preview);
    }

    @Test
    public void extractPreview_shouldStripHorizontalRules() throws Exception {
        String text = "line 1" +
                "------------------------------\r\n" +
                "line 2";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("line 1 line 2", preview);
    }

    @Test
    public void extractPreview_shouldReplaceUrl() throws Exception {
        String text = "some url: https://k9mail.org/";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("some url: ...", preview);
    }

    @Test
    public void extractPreview_shouldCollapseAndTrimWhitespace() throws Exception {
        String text = " whitespace     is\t\tfun  ";
        Part part = createTextPart("text/plain", text);

        String preview = previewTextExtractor.extractPreview(part);

        assertEquals("whitespace is fun", preview);
    }
}
