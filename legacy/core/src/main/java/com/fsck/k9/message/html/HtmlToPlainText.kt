package com.fsck.k9.message.html

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

/**
 * Convert an HTML element to plain text.
 *
 * Based on Jsoup's HtmlToPlainText example.
 */
object HtmlToPlainText {
    @JvmStatic
    fun toPlainText(element: Element): String {
        val formatter = FormattingVisitor()
        NodeTraversor.traverse(formatter, element)

        return formatter.toString()
    }
}

private class FormattingVisitor : NodeVisitor {
    private val output = StringBuilder()
    private var collectLinkText = false
    private var linkText = StringBuilder()

    override fun head(node: Node, depth: Int) {
        val name = node.nodeName()
        when {
            node is TextNode -> {
                val text = node.text()
                append(text)

                if (collectLinkText) {
                    linkText.append(text)
                }
            }
            name == "li" -> {
                startNewLine()
                append("* ")
            }
            name == "a" && node.hasAttr("href") -> {
                collectLinkText = true
                linkText.clear()
            }
            node is Element && node.isBlock -> startNewLine()
        }
    }

    override fun tail(node: Node, depth: Int) {
        val name = node.nodeName()
        when {
            name == "li" -> append("\n")
            name == "br" -> append("\n")
            node is Element && node.isBlock -> {
                if (node.hasText()) {
                    addEmptyLine()
                }
            }
            name == "a" && node.hasAttr("href") -> {
                collectLinkText = false

                if (node.absUrl("href").isNotEmpty()) {
                    if (linkText.toString() != node.attr("href")) {
                        append(" <${node.attr("href")}>")
                    }
                }
            }
        }
    }

    private fun append(text: String) {
        if (text == " " && (output.isEmpty() || output.last() in listOf(' ', '\n'))) {
            return
        }

        output.append(text)
    }

    private fun startNewLine() {
        if (output.isEmpty() || output.last() == '\n') {
            return
        }

        append("\n")
    }

    private fun addEmptyLine() {
        if (output.isEmpty() || output.endsWith("\n\n")) {
            return
        }

        startNewLine()
        append("\n")
    }

    override fun toString(): String {
        if (output.isEmpty()) {
            return ""
        }

        var lastIndex = output.lastIndex
        while (lastIndex >= 0 && output[lastIndex] == '\n') {
            lastIndex--
        }

        return output.substring(0, lastIndex + 1)
    }
}
