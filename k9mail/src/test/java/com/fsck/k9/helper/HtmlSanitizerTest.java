package com.fsck.k9.helper;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class HtmlSanitizerTest {
    private HtmlSanitizer htmlSanitizer;

    @Before
    public void setUp() throws Exception {
        htmlSanitizer = HtmlSanitizer.getInstance();
    }

    @Test
    public void shouldRemoveMetaRefreshInHead() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshBetweenHeadAndBody() {
        String html = "<html>" +
                "<head></head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshInBody() {
        String html = "<html>" +
                "<head></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshWithUpperCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"REFRESH\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshWithMixedCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"Refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshWithoutQuotesAroundAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=refresh content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshWithSpacesInAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh \" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMultipleMetaRefreshTags() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";
        assertEquals("<html><head></head><body>Message</body></html>", htmlSanitizer.sanitize(html));
    }

    @Test
    public void shouldRemoveMetaRefreshButKeepOtherMetaTags() {
        String html = "<html>" +
                "<head>" +
                "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">" +
                "<meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">" +
                "</head>" +
                "<body>Message</body>" +
                "</html>";
        assertEquals("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" /></head>" +
                "<body>Message</body></html>", htmlSanitizer.sanitize(html));
    }
}
