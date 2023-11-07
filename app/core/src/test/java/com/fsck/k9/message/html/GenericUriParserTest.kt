package com.fsck.k9.message.html

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import org.junit.Test

class GenericUriParserTest {
    private val parser = GenericUriParser()

    @Test
    fun `mailto URIs`() {
        // Examples from RFC 6068
        assertUriValid("mailto:chris@example.com")
        assertUriValid("mailto:infobot@example.com?subject=current-issue")
        assertUriValid("mailto:infobot@example.com?body=send%20current-issue")
        assertUriValid("mailto:infobot@example.com?body=send%20current-issue%0D%0Asend%20index")
        assertUriValid("mailto:list@example.org?In-Reply-To=%3C3469A91.D10AF4C@example.com%3E")
        assertUriValid("mailto:majordomo@example.com?body=subscribe%20bamboo-l")
        assertUriValid("mailto:joe@example.com?cc=bob@example.com&body=hello")
        assertUriValid("mailto:gorby%25kremvax@example.com")
        assertUriValid("mailto:unlikely%3Faddress@example.com?blat=foop")
        assertUriValid("mailto:%22not%40me%22@example.org")
        assertUriValid("mailto:%22oh%5C%5Cno%22@example.org")
        assertUriValid("mailto:%22%5C%5C%5C%22it's%5C%20ugly%5C%5C%5C%22%22@example.org")
        assertUriValid("mailto:user@example.org?subject=caf%C3%A9")
        assertUriValid("mailto:user@example.org?subject=%3D%3Futf-8%3FQ%3Fcaf%3DC3%3DA9%3F%3D")
        assertUriValid("mailto:user@example.org?subject=%3D%3Fiso-8859-1%3FQ%3Fcaf%3DE9%3F%3D")
        assertUriValid("mailto:user@example.org?subject=caf%C3%A9&body=caf%C3%A9")
        assertUriValid("mailto:user@%E7%B4%8D%E8%B1%86.example.org?subject=Test&body=NATTO")
    }

    @Test
    fun `XMPP URIs`() {
        // Examples from RFC 5122
        assertUriValid("xmpp:node@example.com")
        assertUriValid("xmpp://guest@example.com")
        assertUriValid("xmpp://guest@example.com/support@example.com?message")
        assertUriValid("xmpp:support@example.com?message")
        assertUriValid("xmpp:example-node@example.com/some-resource")
        assertUriValid("xmpp:example.com")
        assertUriValid("xmpp:example-node@example.com?message")
        assertUriValid("xmpp:example-node@example.com?message;subject=Hello%20World")
        assertUriValid("xmpp:nasty!%23\$%25()*+,-.;=%3F%5B%5C%5D%5E_%60%7B%7C%7D~node@example.com")
        assertUriValid(
            "xmpp:node@example.com/repulsive" +
                "%20!%23%22\$%25&'()*+,-.%2F:;%3C=%3E%3F%40%5B%5C%5D%5E_%60%7B%7C%7D~resource",
        )
        assertUriValid("xmpp:ji%C5%99i@%C4%8Dechy.example/v%20Praze")
    }

    @Test
    fun `matrix URIs`() {
        // Examples from MSC 2312
        assertUriValid("matrix:r/someroom:example.org")
        assertUriValid("matrix:u/me:example.org")
        assertUriValid("matrix:r/someroom:example.org/e/Arbitrary_Event_Id")
        assertUriValid("matrix:u/her:example.org")
        assertUriValid("matrix:u/her:example.org?action=chat")
        assertUriValid("matrix:roomid/rid:example.org")
        assertUriValid("matrix:r/us:example.org")
        assertUriValid("matrix:roomid/rid:example.org?action=join&via=example2.org")
        assertUriValid("matrix:r/us:example.org?action=join")
        assertUriValid("matrix:r/us:example.org/e/lol823y4bcp3qo4")
        assertUriValid("matrix:roomid/rid:example.org/event/lol823y4bcp3qo4?via=example2.org")
    }

    @Test
    fun `negative 'startPos' value`() {
        assertFailure {
            parser.parseUri("test", -1)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Invalid 'startPos' value")
    }

    @Test
    fun `out of bounds 'startPos' value`() {
        assertFailure {
            parser.parseUri("test", 4)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Invalid 'startPos' value")
    }

    private fun assertUriValid(input: String) {
        val result = parser.parseUri(input, 0)

        assertThat(result).isNotNull()
            .prop(UriMatch::uri).isEqualTo(input)
    }
}
