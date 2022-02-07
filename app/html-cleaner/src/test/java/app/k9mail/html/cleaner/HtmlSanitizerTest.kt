package app.k9mail.html.cleaner

import com.google.common.truth.Truth.assertThat
import org.jsoup.nodes.Document
import org.junit.Test

class HtmlSanitizerTest {
    private val htmlSanitizer = HtmlSanitizer()

    @Test
    fun shouldRemoveMetaRefreshInHead() {
        val html =
            """
            <html>
            <head><meta http-equiv="refresh" content="1; URL=http://example.com/"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshBetweenHeadAndBody() {
        val html =
            """
            <html>
            <head></head>
            <meta http-equiv="refresh" content="1; URL=http://example.com/">
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshInBody() {
        val html =
            """
            <html>
            <head></head>
            <body><meta http-equiv="refresh" content="1; URL=http://example.com/">Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshWithUpperCaseAttributeValue() {
        val html =
            """
            <html>
            <head><meta http-equiv="REFRESH" content="1; URL=http://example.com/"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshWithMixedCaseAttributeValue() {
        val html =
            """
            <html>
            <head><meta http-equiv="Refresh" content="1; URL=http://example.com/"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshWithoutQuotesAroundAttributeValue() {
        val html =
            """
            <html>
            <head><meta http-equiv=refresh content="1; URL=http://example.com/"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshWithSpacesInAttributeValue() {
        val html =
            """
            <html>
            <head><meta http-equiv="refresh " content="1; URL=http://example.com/"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMultipleMetaRefreshTags() {
        val html =
            """
            <html>
            <head><meta http-equiv="refresh" content="1; URL=http://example.com/"></head>
            <body><meta http-equiv="refresh" content="1; URL=http://example.com/">Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>Message</body></html>")
    }

    @Test
    fun shouldRemoveMetaRefreshButKeepOtherMetaTags() {
        val html =
            """
            <html>
            <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <meta http-equiv="refresh" content="1; URL=http://example.com/">
            </head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <html>
            <head><meta http-equiv="content-type" content="text/html; charset=UTF-8"></head>
            <body>Message</body>
            </html>
            """.trimIndent().trimLineBreaks()
        )
    }

    @Test
    fun shouldProduceValidHtmlFromHtmlWithXmlDeclaration() {
        val html =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <html>
            <head></head>
            <body></body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body></body></html>")
    }

    @Test
    fun shouldNormalizeTables() {
        val html = "<html><head></head><body><table><tr><td></td><td></td></tr></table></body></html>"

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            "<html><head></head><body><table><tbody><tr><td></td><td></td></tr></tbody></table></body></html>"
        )
    }

    @Test
    fun shouldHtmlEncodeXmlDirectives() {
        val html =
            """
            <html>
            <head></head>
            <body>
            <table><tr><td><!==><!==>Hmailserver service shutdown:</td><td><!==><!==>Ok</td></tr></table>
            </body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <html>
            <head></head>
            <body><table><tbody><tr><td>Hmailserver service shutdown:</td><td>Ok</td></tr></tbody></table></body>
            </html>
            """.trimIndent().trimLineBreaks()
        )
    }

    @Test
    fun shouldKeepHrTags() {
        val html = "<html><head></head><body>one<hr>two<hr />three</body></html>"

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body>one<hr>two<hr>three</body></html>")
    }

    @Test
    fun shouldKeepInsDelTags() {
        val html = "<html><head></head><body><ins>Inserted</ins><del>Deleted</del></body></html>"

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(html)
    }

    @Test
    fun shouldKeepMapAreaTags() {
        val html =
            """
            <html>
            <head></head>
            <body>
            <map name="planetmap">
                <area shape="rect" coords="0,0,82,126" href="http://domain.com/sun.htm" alt="Sun">
                <area shape="circle" coords="90,58,3" href="http://domain.com/mercur.htm" alt="Mercury">
                <area shape="circle" coords="124,58,8" href="http://domain.com/venus.htm" alt="Venus">
            </map>
            </body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(html)
    }

    @Test
    fun shouldKeepImgUsemap() {
        val html =
            """
            <html>
            <head></head>
            <body><img src="http://domain.com/image.jpg" usemap="#planetmap"></body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(html)
    }

    @Test
    fun shouldKeepAllowedElementsInHeadAndSkipTheRest() {
        val html =
            """
            <html>
            <head>
            <title>remove this</title>
            <style>keep this</style>
            <script>remove this</script>
            </head>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head><style>keep this</style></head><body></body></html>")
    }

    @Test
    fun shouldRemoveIFrames() {
        val html = """<html><body><iframe src="http://www.google.com" /></body></html>"""

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo("<html><head></head><body></body></html>")
    }

    @Test
    fun shouldKeepFormattingTags() {
        val html = """<html><body><center><font face="Arial" color="red" size="12">A</font></center></body></html>"""

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <html>
            <head></head>
            <body><center><font face="Arial" color="red" size="12">A</font></center></body>
            </html>
            """.trimIndent().trimLineBreaks()
        )
    }

    @Test
    fun shouldKeepUris() {
        val html =
            """
            <html>
            <body>
            <a href="http://example.com/index.html">HTTP</a>
            <a href="https://example.com/default.html">HTTPS</a>
            <a href="mailto:user@example.com">Mailto</a>
            <a href="tel:00442079460111">Telephone</a>
            <a href="sip:user@example.com">SIP</a>
            <a href="bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu">Bitcoin</a>
            <a href="ethereum:0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7">Ethereum</a>
            <a href="rtsp://example.com/media.mp4">RTSP</a>
            </body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <html>
            <head></head>
            <body>
            <a href="http://example.com/index.html">HTTP</a>
            <a href="https://example.com/default.html">HTTPS</a>
            <a href="mailto:user@example.com">Mailto</a>
            <a href="tel:00442079460111">Telephone</a>
            <a href="sip:user@example.com">SIP</a>
            <a href="bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu">Bitcoin</a>
            <a href="ethereum:0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7">Ethereum</a>
            <a href="rtsp://example.com/media.mp4">RTSP</a>
            </body>
            </html>
            """.trimIndent().trimLineBreaks()
        )
    }

    @Test
    fun shouldKeepDirAttribute() {
        val html =
            """
            <html>
            <head></head>
            <body><table><tbody><tr><td dir="rtl"></td></tr></tbody></table></body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(html)
    }

    @Test
    fun shouldKeepAllowedBodyAttributes() {
        val html =
            """
            <html>
            <body style="color: #fff" onload="alert()" class="body" id></body>
            </html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <html>
            <head></head>
            <body style="color: #fff" class="body" id></body>
            </html>
            """.trimIndent().trimLineBreaks()
        )
    }

    @Test
    fun `should keep HTML 5 doctype`() {
        val html =
            """
            <!doctype html>
            <html><head></head><body>text</body></html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(html)
    }

    @Test
    fun `should keep HTML 4_01 doctype`() {
        val html =
            """
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
            <html><head></head><body>text</body></html>
            """.trimIndent().trimLineBreaks()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result.toCompactString()).isEqualTo(
            """
            <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
            <html><head></head><body>text</body></html>
            """.trimIndent().trimLineBreaks()
        )
    }

    private fun Document.toCompactString(): String {
        outputSettings()
            .prettyPrint(false)
            .indentAmount(0)

        return html()
    }

    private fun String.trimLineBreaks() = replace("\n", "")
}
