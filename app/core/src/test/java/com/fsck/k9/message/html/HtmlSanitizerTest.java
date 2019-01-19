package com.fsck.k9.message.html;


import org.jsoup.nodes.Document;
import org.junit.Test;

import static com.fsck.k9.message.html.HtmlProcessor.toCompactString;
import static org.junit.Assert.assertEquals;


public class HtmlSanitizerTest {
    private HtmlSanitizer htmlSanitizer = new HtmlSanitizer();


    @Test
    public void shouldRemoveMetaRefreshInHead() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshBetweenHeadAndBody() {
        String html = "<html>" +
                "<head></head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshInBody() {
        String html = "<html>" +
                "<head></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshWithUpperCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"REFRESH\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshWithMixedCaseAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"Refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshWithoutQuotesAroundAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=refresh content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMetaRefreshWithSpacesInAttributeValue() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh \" content=\"1; URL=http://example.com/\"></head>" +
                "<body>Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveMultipleMetaRefreshTags() {
        String html = "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\"></head>" +
                "<body><meta http-equiv=\"refresh\" content=\"1; URL=http://example.com/\">Message</body>" +
                "</html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>Message</body></html>", toCompactString(result));
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

        Document result = htmlSanitizer.sanitize(html);


        assertEquals("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head>" +
                "<body>Message</body></html>", toCompactString(result));
    }

    @Test
    public void shouldProduceValidHtmlFromHtmlWithXmlDeclaration() {
        String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<html><head></head><body></body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body></body></html>", toCompactString(result));
    }

    @Test
    public void shouldNormalizeTables() {
        String html = "<html><head></head><body><table><tr><td></td><td></td></tr></table></body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body><table><tbody>" +
                "<tr><td></td><td></td></tr>" +
                "</tbody></table></body></html>", toCompactString(result));
    }

    @Test
    public void shouldHtmlEncodeXmlDirectives() {
        String html = "<html><head></head><body><table>" +
                "<tr><td><!==><!==>Hmailserver service shutdown:</td><td><!==><!==>Ok</td></tr>" +
                "</table></body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body><table><tbody>" +
                "<tr><td>Hmailserver service shutdown:</td><td>Ok</td></tr>" +
                "</tbody></table></body></html>", toCompactString(result));
    }

    @Test
    public void shouldKeepHrTags() throws Exception {
        String html = "<html><head></head><body>one<hr>two<hr />three</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>one<hr>two<hr>three</body></html>", toCompactString(result));
    }

    @Test
    public void shouldKeepInsDelTags() {
        String html = "<html><head></head><body><ins>Inserted</ins><del>Deleted</del></body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals(html, toCompactString(result));
    }

    @Test
    public void shouldKeepMapAreaTags() {
        String html = "<html><head></head><body><map name=\"planetmap\">\n" +
                "  <area shape=\"rect\" coords=\"0,0,82,126\" href=\"http://domain.com/sun.htm\" alt=\"Sun\">\n" +
                "  <area shape=\"circle\" coords=\"90,58,3\" href=\"http://domain.com/mercur.htm\" alt=\"Mercury\">\n" +
                "  <area shape=\"circle\" coords=\"124,58,8\" href=\"http://domain.com/venus.htm\" alt=\"Venus\">\n" +
                "</map></body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals(html, toCompactString(result));
    }

    @Test
    public void shouldKeepImgUsemap() {
        String html = "<html><head></head><body>" +
                "<img src=\"http://domain.com/image.jpg\" usemap=\"#planetmap\">" +
                "</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals(html, toCompactString(result));
    }

    @Test
    public void shouldKeepWhitelistedElementsInHeadAndSkipTheRest() {
        String html = "<html><head>" +
                "<title>remove this</title>" +
                "<style>keep this</style>" +
                "<script>remove this</script>" +
                "</head></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head><style>keep this</style></head><body></body></html>", toCompactString(result));
    }

    @Test
    public void shouldRemoveIFrames() {
        String html = "<html><body>" +
                "<iframe src=\"http://www.google.com\" />" +
                "</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body></body></html>", toCompactString(result));
    }

    @Test
    public void shouldKeepFormattingTags() {
        String html = "<html><body>" +
                "<center><font face=\"Arial\" color=\"red\" size=\"12\">A</font></center>" +
                "</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>" +
                "<center><font face=\"Arial\" color=\"red\" size=\"12\">A</font></center>" +
                "</body></html>", toCompactString(result));
    }

    @Test
    public void shouldKeepUris() {
        String html = "<html><body>" +
                "<a href=\"http://example.com/index.html\">HTTP</a>" +
                "<a href=\"https://example.com/default.html\">HTTPS</a>" +
                "<a href=\"mailto:user@example.com\">Mailto</a>" +
                "<a href=\"tel:00442079460111\">Telephone</a>" +
                "<a href=\"sip:user@example.com\">SIP</a>" +
                "<a href=\"bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu\">Bitcoin</a>" +
                "<a href=\"ethereum:0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7\">Ethereum</a>" +
                "<a href=\"rtsp://example.com/media.mp4\">RTSP</a>" +
                "</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals("<html><head></head><body>" +
                "<a href=\"http://example.com/index.html\">HTTP</a>" +
                "<a href=\"https://example.com/default.html\">HTTPS</a>" +
                "<a href=\"mailto:user@example.com\">Mailto</a>" +
                "<a href=\"tel:00442079460111\">Telephone</a>" +
                "<a href=\"sip:user@example.com\">SIP</a>" +
                "<a href=\"bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu\">Bitcoin</a>" +
                "<a href=\"ethereum:0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7\">Ethereum</a>" +
                "<a href=\"rtsp://example.com/media.mp4\">RTSP</a>" +
                "</body></html>", toCompactString(result));
    }

    @Test
    public void shouldKeepDirAttribute() {
        String html = "<html><head></head><body>" +
                "<table><tbody><tr><td dir=\"rtl\"></td></tr></tbody></table>" +
                "</body></html>";

        Document result = htmlSanitizer.sanitize(html);

        assertEquals(html, toCompactString(result));
    }
}
