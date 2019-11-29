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
    private var width = 0
    private val output = StringBuilder()

    override fun head(node: Node, depth: Int) {
        val name = node.nodeName()
        when {
            node is TextNode -> append(node.text())
            name == "li" -> {
                startNewLine()
                append("* ")
            }
            node is Element && node.isBlock -> startNewLine()
        }
    }

    override fun tail(node: Node, depth: Int) {
        val name = node.nodeName()
        when {
            name == "li" -> append("\n")
            node is Element && node.isBlock -> {
                if (node.hasText()) {
                    addEmptyLine()
                }
            }
            name == "a" -> {
                if (node.absUrl("href").isNotEmpty()) {
                    append(" <${node.attr("href")}>")
                }
            }
        }
    }

    private fun append(text: String) {
        if (text.startsWith("\n")) {
            width = 0
        }

        if (text == " " && (output.isEmpty() || output.last() in listOf(' ', '\n'))) {
            return
        }

        if (text.length + width > MAX_WIDTH) {
            val words = text.split(Regex("\\s+"))
            for (i in words.indices) {
                var word = words[i]

                val last = i == words.size - 1
                if (!last) {
                    word = "$word "
                }

                if (word.length + width > MAX_WIDTH) {
                    output.append("\n").append(word)
                    width = word.length
                } else {
                    output.append(word)
                    width += word.length
                }
            }
        } else {
            output.append(text)
            width += text.length
        }
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

    companion object {
        private const val MAX_WIDTH = 76
    }
}
