package com.fsck.k9.message.signature

import com.fsck.k9.helper.jsoup.AdvancedNodeTraversor
import com.fsck.k9.helper.jsoup.NodeFilter
import com.fsck.k9.helper.jsoup.NodeFilter.HeadFilterDecision
import com.fsck.k9.helper.jsoup.NodeFilter.TailFilterDecision
import java.util.regex.Pattern
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag

class HtmlSignatureRemover {
    private fun stripSignatureInternal(content: String): String {
        val document = Jsoup.parse(content)

        val nodeTraversor = AdvancedNodeTraversor(StripSignatureFilter())
        nodeTraversor.filter(document.body())

        return toCompactString(document)
    }

    private fun toCompactString(document: Document): String {
        document.outputSettings()
            .prettyPrint(false)
            .indentAmount(0)

        return document.html()
    }

    private class StripSignatureFilter : NodeFilter {
        private var signatureFound = false
        private var lastElementCausedLineBreak = false
        private var brElementPrecedingDashes: Element? = null

        override fun head(node: Node, depth: Int): HeadFilterDecision {
            if (signatureFound) return HeadFilterDecision.REMOVE

            if (node is Element) {
                lastElementCausedLineBreak = false
                if (node.tag() == BLOCKQUOTE) {
                    return HeadFilterDecision.SKIP_ENTIRELY
                }
            } else if (node is TextNode) {
                if (lastElementCausedLineBreak && DASH_SIGNATURE_HTML.matcher(node.wholeText).matches()) {
                    val nextNode = node.nextSibling()
                    if (nextNode is Element && nextNode.tag() == BR) {
                        signatureFound = true
                        brElementPrecedingDashes?.remove()
                        brElementPrecedingDashes = null

                        return HeadFilterDecision.REMOVE
                    }
                }
            }

            return HeadFilterDecision.CONTINUE
        }

        override fun tail(node: Node, depth: Int): TailFilterDecision {
            if (signatureFound) return TailFilterDecision.CONTINUE

            if (node is Element) {
                val elementIsBr = node.tag() == BR
                if (elementIsBr || node.tag() == P) {
                    lastElementCausedLineBreak = true
                    brElementPrecedingDashes = if (elementIsBr) node else null

                    return TailFilterDecision.CONTINUE
                }
            }

            lastElementCausedLineBreak = false
            return TailFilterDecision.CONTINUE
        }
    }

    companion object {
        private val DASH_SIGNATURE_HTML = Pattern.compile("\\s*-- \\s*", Pattern.CASE_INSENSITIVE)
        private val BLOCKQUOTE = Tag.valueOf("blockquote")
        private val BR = Tag.valueOf("br")
        private val P = Tag.valueOf("p")

        @JvmStatic
        fun stripSignature(content: String): String {
            return HtmlSignatureRemover().stripSignatureInternal(content)
        }
    }
}
