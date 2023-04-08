package com.fsck.k9.message.signature

import com.fsck.k9.helper.jsoup.AdvancedNodeTraversor
import com.fsck.k9.helper.jsoup.NodeFilter
import com.fsck.k9.helper.jsoup.NodeFilter.HeadFilterDecision
import com.fsck.k9.helper.jsoup.NodeFilter.TailFilterDecision
import java.util.Stack
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
        private var signatureParentNode: Node? = null

        override fun head(node: Node, depth: Int): HeadFilterDecision {
            if (signatureFound) return HeadFilterDecision.REMOVE

            if (node.isBlockquote()) {
                return HeadFilterDecision.SKIP_ENTIRELY
            } else if (node.isSignatureDelimiter()) {
                val precedingLineBreak = node.findPrecedingLineBreak()
                if (precedingLineBreak != null && node.isFollowedByLineBreak()) {
                    signatureFound = true
                    signatureParentNode = node.parent()
                    precedingLineBreak.takeIf { it.isBR() }?.remove()

                    return HeadFilterDecision.REMOVE
                }
            }

            return HeadFilterDecision.CONTINUE
        }

        override fun tail(node: Node, depth: Int): TailFilterDecision {
            if (signatureFound) {
                val signatureParentNode = this.signatureParentNode
                if (node == signatureParentNode) {
                    return if (signatureParentNode.isEmpty()) {
                        this.signatureParentNode = signatureParentNode.parent()
                        TailFilterDecision.REMOVE
                    } else {
                        TailFilterDecision.STOP
                    }
                }
            }

            return TailFilterDecision.CONTINUE
        }

        private fun Node.isBlockquote(): Boolean {
            return this is Element && tag() == BLOCKQUOTE
        }

        private fun Node.isSignatureDelimiter(): Boolean {
            return this is TextNode && DASH_SIGNATURE_HTML.matcher(wholeText).matches()
        }

        private fun Node.findPrecedingLineBreak(): Node? {
            val stack = Stack<Node>()
            stack.push(this)

            while (stack.isNotEmpty()) {
                val node = stack.pop()
                val previousSibling = node.previousSibling()
                if (previousSibling == null) {
                    val parent = node.parent()
                    if (parent is Element && parent.isBlock) {
                        return parent
                    } else {
                        stack.push(parent)
                    }
                } else if (previousSibling.isLineBreak()) {
                    return previousSibling
                }
            }

            return null
        }

        private fun Node.isFollowedByLineBreak(): Boolean {
            val stack = Stack<Node>()
            stack.push(this)

            while (stack.isNotEmpty()) {
                val node = stack.pop()
                val nextSibling = node.nextSibling()
                if (nextSibling == null) {
                    val parent = node.parent()
                    if (parent is Element && parent.isBlock) {
                        return true
                    } else {
                        stack.push(parent)
                    }
                } else if (nextSibling.isLineBreak()) {
                    return true
                }
            }

            return false
        }

        private fun Node?.isBR() = this is Element && tag() == BR

        private fun Node?.isLineBreak() = isBR() || (this is Element && this.isBlock)

        private fun Node.isEmpty(): Boolean = childNodeSize() == 0
    }

    companion object {
        private val DASH_SIGNATURE_HTML = Pattern.compile("\\s*--[ \u00A0]\\s*")
        private val BLOCKQUOTE = Tag.valueOf("blockquote")
        private val BR = Tag.valueOf("br")

        @JvmStatic
        fun stripSignature(content: String): String {
            return HtmlSignatureRemover().stripSignatureInternal(content)
        }
    }
}
