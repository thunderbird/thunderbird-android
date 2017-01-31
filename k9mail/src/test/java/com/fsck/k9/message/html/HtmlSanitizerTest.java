package com.fsck.k9.message.html;


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

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshBetweenHeadAndBody() {
        String html = "<html>" +
                "<head></head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">" +
                "<body>Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshInBody() {
        String html = "<html>" +
                "<head></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshWithUpperCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"REFRESH\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshWithMixedCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"Refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshWithoutQuotesAroundAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=refresh content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMetaRefreshWithSpacesInAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh \" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
    }

    @Test
    public void shouldRemoveMultipleMetaRefreshTags() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", result);
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

        String result = htmlSanitizer.sanitize(html);


        assertEquals("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" /></head>" +
                "<body>Message</body></html>", result);
    }

    @Test
    public void shouldProduceValidHtmlFromHtmlWithXmlDeclaration() {
        String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<html><head></head><body></body></html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body></body></html>", result);
    }

    @Test
    public void shouldNormalizeTables() {
        String html = "<html><head></head><body><table><tr><td></td><td></td></tr></table></body></html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body><table><tbody>" +
                "<tr><td></td><td></td></tr>" +
                "</tbody></table></body></html>", result);
    }

    @Test
    public void shouldHtmlEncodeXmlDirectives() {
        String html = "<html><head></head><body><table>" +
                "<tr><td><!==><!==>Hmailserver service shutdown:</td><td><!==><!==>Ok</td></tr>" +
                "</table></body></html>";

        String result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body><table><tbody>" +
                "<tr><td>&lt;!==&gt;&lt;!==&gt;Hmailserver service shutdown:</td><td>&lt;!==&gt;&lt;!==&gt;Ok</td></tr>" +
                "</tbody></table></body></html>", result);
    }
}
