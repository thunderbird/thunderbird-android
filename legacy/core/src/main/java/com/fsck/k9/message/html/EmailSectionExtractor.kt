package com.fsck.k9.message.html

/**
 * Extract sections from a plain text email.
 *
 * A section consists of all consecutive lines of the same quote depth. Quote characters and spaces at the beginning of
 * a line are stripped and not part of the section's content.
 *
 * ### Example:
 *
 * ```
 * On 2018-01-25 Alice <alice@example.com> wrote:
 * > Hi Bob
 *
 * Hi Alice
 * ```
 *
 * This message consists of three sections with the following contents:
 * * `On 2018-01-25 Alice <alice@example.com> wrote:`
 * * `Hi Bob`
 * * `Hi Alice`
 */
class EmailSectionExtractor private constructor(val text: String) {
    private val sections = mutableListOf<EmailSection>()
    private var sectionBuilder = EmailSection.Builder(text, 0)
    private var sectionStartIndex = 0
    private var newlineIndex = -1
    private var startOfContentIndex = 0
    private var isStartOfLine = true
    private var spaces = 0
    private var quoteDepth = 0
    private var currentQuoteDepth = 0

    fun extract(): List<EmailSection> {
        text.forEachIndexed { index, character ->
            if (isStartOfLine) {
                detectQuoteCharacters(index, character)
            } else if (character == '\n') {
                addQuotedLineToSection(endIndex = index + 1)
            }

            if (character == '\n') {
                newlineIndex = index
                resetForStartOfLine()
            }
        }

        completeLastSection()

        return sections
    }

    private fun detectQuoteCharacters(index: Int, character: Char) {
        when (character) {
            ' ' -> spaces++
            '>' -> {
                if (quoteDepth == 0 && currentQuoteDepth == 0) {
                    addUnquotedLineToSection(newlineIndex + 1)
                }
                currentQuoteDepth++
                spaces = 0
            }
            '\n' -> {
                if (quoteDepth != currentQuoteDepth) {
                    finishSection()
                    sectionStartIndex = index - spaces
                }
                if (currentQuoteDepth > 0) {
                    sectionBuilder.addBlankSegment(startIndex = index - spaces, endIndex = index + 1)
                }
            }
            else -> {
                isStartOfLine = false
                startOfContentIndex = index - spaces
                if (quoteDepth != currentQuoteDepth) {
                    finishSection()
                    sectionStartIndex = startOfContentIndex
                }
            }
        }
    }

    private fun addUnquotedLineToSection(endIndex: Int) {
        if (sectionStartIndex != endIndex) {
            sectionBuilder.addSegment(0, sectionStartIndex, endIndex)
        }
    }

    private fun addQuotedLineToSection(startIndex: Int = startOfContentIndex, endIndex: Int) {
        if (currentQuoteDepth > 0) {
            sectionBuilder.addSegment(spaces, startIndex, endIndex)
        }
    }

    private fun finishSection() {
        appendSection()
        sectionBuilder = EmailSection.Builder(text, currentQuoteDepth)
        quoteDepth = currentQuoteDepth
    }

    private fun completeLastSection() {
        if (quoteDepth == 0) {
            sectionBuilder.addSegment(0, sectionStartIndex, text.length)
        } else if (!isStartOfLine) {
            sectionBuilder.addSegment(spaces, startOfContentIndex, text.length)
        }

        appendSection()
    }

    private fun appendSection() {
        if (sectionBuilder.hasSegments) {
            sections.add(sectionBuilder.build())
        }
    }

    private fun resetForStartOfLine() {
        isStartOfLine = true
        currentQuoteDepth = 0
        spaces = 0
    }

    companion object {
        fun extract(text: String) = EmailSectionExtractor(text).extract()
    }
}
