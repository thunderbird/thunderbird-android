package com.fsck.k9.message.signature;


import com.fsck.k9.K9RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.fsck.k9.message.html.HtmlHelper.extractText;
import static org.junit.Assert.assertEquals;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HtmlSignatureRemoverTest {
    @Test
    public void shouldStripSignatureFromK9StyleHtml() throws Exception {
        String html = "This is the body text" +
                "<br>" +
                "-- <br>" +
                "Sent from my Android device with K-9 Mail. Please excuse my brevity.";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("This is the body text", extractText(withoutSignature));
    }

    @Test
    public void shouldStripSignatureFromThunderbirdStyleHtml() throws Exception {
        String html = "<html>\r\n" +
                "  <head>\r\n" +
                "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\r\n" +
                "  </head>\r\n" +
                "  <body bgcolor=\"#FFFFFF\" text=\"#000000\">\r\n" +
                "    <p>This is the body text<br>\r\n" +
                "    </p>\r\n" +
                "    -- <br>\r\n" +
                "    <div class=\"moz-signature\">Sent from my Android device with K-9 Mail." +
                " Please excuse my brevity.</div>\r\n" +
                "  </body>\r\n" +
                "</html>";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("This is the body text", extractText(withoutSignature));
    }

    @Test
    public void shouldStripSignatureBeforeBlockquoteTag() throws Exception {
        String html = "<html><head></head><body>" +
                "<div>" +
                "This is the body text" +
                "<br>" +
                "-- <br>" +
                "<blockquote>" +
                "Sent from my Android device with K-9 Mail. Please excuse my brevity." +
                "</blockquote>" +
                "</div>" +
                "</body></html>";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("<html><head></head><body>" +
                        "<div>This is the body text</div>" +
                        "</body></html>",
                withoutSignature);
    }

    @Test
    public void shouldNotStripSignatureInsideBlockquoteTags() throws Exception {
        String html = "<html><head></head><body>" +
                "<blockquote>" +
                "This is some quoted text" +
                "<br>" +
                "-- <br>" +
                "Inner signature" +
                "</blockquote>" +
                "<div>" +
                "This is the body text" +
                "</div>" +
                "</body></html>";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("<html><head></head><body>" +
                        "<blockquote>" +
                        "This is some quoted text" +
                        "<br>" +
                        "-- <br>" +
                        "Inner signature" +
                        "</blockquote>" +
                        "<div>This is the body text</div>" +
                        "</body></html>",
                withoutSignature);
    }

    @Test
    public void shouldStripSignatureBetweenBlockquoteTags() throws Exception {
        String html = "<html><head></head><body>" +
                "<blockquote>" +
                "Some quote" +
                "</blockquote>" +
                "<div>" +
                "This is the body text" +
                "<br>" +
                "-- <br>" +
                "<blockquote>" +
                "Sent from my Android device with K-9 Mail. Please excuse my brevity." +
                "</blockquote>" +
                "<br>" +
                "-- <br>" +
                "Signature inside signature" +
                "</div>" +
                "</body></html>";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("<html><head></head><body>" +
                        "<blockquote>Some quote</blockquote>" +
                        "<div>This is the body text</div>" +
                        "</body></html>",
                withoutSignature);
    }

    @Test
    public void shouldStripSignatureAfterLastBlockquoteTags() throws Exception {
        String html = "<html><head></head><body>" +
                "This is the body text" +
                "<br>" +
                "<blockquote>" +
                "Some quote" +
                "</blockquote>" +
                "<br>" +
                "-- <br>" +
                "Sent from my Android device with K-9 Mail. Please excuse my brevity." +
                "</body></html>";

        String withoutSignature = HtmlSignatureRemover.stripSignature(html);

        assertEquals("<html><head></head><body>" +
                        "This is the body text<br>" +
                        "<blockquote>Some quote</blockquote>" +
                        "</body></html>",
                withoutSignature);
    }
}
