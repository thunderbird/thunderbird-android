package com.fsck.k9.message.html

internal object DividerReplacer : TextToHtml.HtmlModifier {
    private const val SIMPLE_DIVIDER = "[-=_]{3,}"
    private const val ASCII_SCISSORS = "(?:-{2,}\\s?(?:>[%8]|[%8]<)\\s?-{2,})+"
    private val PATTERN = Regex(
        "(?:^|\\n)" +
            "(?:" +
            "\\s*" +
            "(?:" + SIMPLE_DIVIDER + "|" + ASCII_SCISSORS + ")" +
            "\\s*" +
            "(?:\\n|$)" +
            ")+",
    )

    override fun findModifications(text: CharSequence): List<HtmlModification> {
        return PATTERN.findAll(text).map { matchResult ->
            Divider(matchResult.range.start, matchResult.range.endInclusive + 1)
        }.toList()
    }

    class Divider(startIndex: Int, endIndex: Int) : HtmlModification.Replace(startIndex, endIndex) {
        override fun replace(textToHtml: TextToHtml) {
            textToHtml.appendHtml("<hr>")
        }

        override fun toString(): String {
            return "Divider{startIndex=$startIndex, endIndex=$endIndex}"
        }
    }
}
