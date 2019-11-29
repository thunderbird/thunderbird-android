package com.fsck.k9.ui.helper

import android.text.Editable
import android.text.Html
import android.text.Html.TagHandler
import android.text.Spanned
import org.xml.sax.XMLReader

/**
 * Convert HTML to a [Spanned] that can be used in a [android.widget.TextView].
 */
class HtmlToSpanned {
    fun convert(html: String): Spanned {
        return Html.fromHtml(html, null, ListTagHandler())
    }
}

/**
 * [TagHandler] that supports unordered lists.
 */
private class ListTagHandler : TagHandler {
    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag == "ul") {
            if (opening) {
                var lastChar: Char = 0.toChar()
                if (output.isNotEmpty()) {
                    lastChar = output[output.length - 1]
                }
                if (lastChar != '\n') {
                    output.append("\r\n")
                }
            } else {
                output.append("\r\n")
            }
        }

        if (tag == "li") {
            if (opening) {
                output.append("\t•  ")
            } else {
                output.append("\r\n")
            }
        }
    }
}
