package com.fsck.k9.message.html

internal object SignatureWrapper : TextToHtml.HtmlModifier {
    private val SIGNATURE_REGEX = "(?m)^-- $".toRegex()

    override fun findModifications(text: CharSequence): List<HtmlModification> {
        val matchResult = SIGNATURE_REGEX.find(text) ?: return emptyList()
        return listOf(Signature(matchResult.range.first, text.length))
    }

    class Signature(startIndex: Int, endIndex: Int) : HtmlModification.Wrap(startIndex, endIndex) {
        override fun appendPrefix(textToHtml: TextToHtml) {
            textToHtml.appendHtml("<div class='k9mail-signature'>")
        }

        override fun appendSuffix(textToHtml: TextToHtml) {
            textToHtml.appendHtml("</div>")
        }

        override fun toString(): String {
            return "Signature{startIndex=$startIndex, endIndex=$endIndex}"
        }
    }
}
