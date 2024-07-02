package com.fsck.k9.message.html

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class EmailSectionExtractorTest {
    @Test
    fun simpleMessageWithoutQuotes() {
        val message =
            """
            Hi Alice,

            are we still on for new Thursday?

            Best
            Bob
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(1)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo(message)
        }
    }

    @Test
    fun simpleMessageEndingWithTwoNewlines() {
        val message = "Hello\n\n"

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(1)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo(message)
        }
    }

    @Test
    fun quoteFollowedByReply() {
        val message =
            """
            Alice <alice@example.org> wrote:
            > Hi there

            Hi, what's up?
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(3)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo("Alice <alice@example.org> wrote:\n")
        }
        with(sections[1]) {
            assertThat(quoteDepth).isEqualTo(1)
            assertThat(toString()).isEqualTo("Hi there\n")
        }
        with(sections[2]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo("\nHi, what's up?")
        }
    }

    @Test
    fun replyFollowedByTwoQuoteLevels() {
        val message =
            """
            Three

            Bob <bob@example.org> wrote:
            > Two
            >${" "}
            > Alice <alice@example.org> wrote:
            >> One
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(3)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo("Three\n\nBob <bob@example.org> wrote:\n")
        }
        with(sections[1]) {
            assertThat(quoteDepth).isEqualTo(1)
            assertThat(toString()).isEqualTo("Two\n\nAlice <alice@example.org> wrote:\n")
        }
        with(sections[2]) {
            assertThat(quoteDepth).isEqualTo(2)
            assertThat(toString()).isEqualTo("One")
        }
    }

    @Test
    fun quoteEndingWithEmptyLineButNoNewline() {
        val message =
            """
            > Quoted text
            > 
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(1)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(1)
            // Note: "Quoted text\n\n" would be a better representation of the quoted text. The goal of this test is
            // not to preserve the current behavior of only ending in one newline, but to make sure we don't add the
            // last line twice.
            assertThat(toString()).isEqualTo("Quoted text\n")
        }
    }

    @Test
    fun chaosQuoting() {
        val message =
            """
            >>> One
            > Three
            Four
            >> Two${"\n"}
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(4)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(3)
            assertThat(toString()).isEqualTo("One\n")
        }
        with(sections[1]) {
            assertThat(quoteDepth).isEqualTo(1)
            assertThat(toString()).isEqualTo("Three\n")
        }
        with(sections[2]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo("Four\n")
        }
        with(sections[3]) {
            assertThat(quoteDepth).isEqualTo(2)
            assertThat(toString()).isEqualTo("Two\n")
        }
    }

    @Test
    fun quotedSectionStartingWithEmptyLine() {
        val message =
            """
            Quote header:
            > 
            > Quoted text
            """.trimIndent()

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(2)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(0)
            assertThat(toString()).isEqualTo("Quote header:\n")
        }
        with(sections[1]) {
            assertThat(quoteDepth).isEqualTo(1)
            assertThat(toString()).isEqualTo("\nQuoted text")
        }
    }

    @Test
    fun quotedBlankLinesShouldNotContributeToIndentValue() {
        val message = "" +
            ">\n" +
            ">  Quoted text\n" +
            ">   \n" +
            "> \n" +
            ">  More quoted text"

        val sections = EmailSectionExtractor.extract(message)

        assertThat(sections.size).isEqualTo(1)
        with(sections[0]) {
            assertThat(quoteDepth).isEqualTo(1)
            assertThat(toString()).isEqualTo("\nQuoted text\n \n\nMore quoted text")
        }
    }
}
