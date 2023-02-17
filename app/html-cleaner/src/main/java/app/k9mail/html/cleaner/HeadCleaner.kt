package app.k9mail.html.cleaner

import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

private val ALLOWED_TAGS = listOf("style", "meta", "base")

internal class HeadCleaner {
    fun clean(dirtyDocument: Document, cleanedDocument: Document) {
        copySafeNodes(dirtyDocument.head(), cleanedDocument.head())
    }

    private fun copySafeNodes(source: Element, destination: Element) {
        val cleaningVisitor = CleaningVisitor(source, destination)
        NodeTraversor.traverse(cleaningVisitor, source)
    }
}

internal class CleaningVisitor(
    private val root: Element,
    private var destination: Element,
) : NodeVisitor {
    private var elementToSkip: Element? = null

    override fun head(source: Node, depth: Int) {
        if (elementToSkip != null) return

        if (source is Element) {
            if (isSafeTag(source)) {
                val sourceTag = source.tagName()
                val destinationAttributes = source.attributes().clone()
                val destinationChild = Element(Tag.valueOf(sourceTag), source.baseUri(), destinationAttributes)
                destination.appendChild(destinationChild)
                destination = destinationChild
            } else if (source !== root) {
                elementToSkip = source
            }
        } else if (source is TextNode) {
            val destinationText = TextNode(source.wholeText)
            destination.appendChild(destinationText)
        } else if (source is DataNode && isSafeTag(source.parent())) {
            val destinationData = DataNode(source.wholeData)
            destination.appendChild(destinationData)
        }
    }

    override fun tail(source: Node, depth: Int) {
        if (source === elementToSkip) {
            elementToSkip = null
        } else if (source is Element && isSafeTag(source)) {
            destination = destination.parent() ?: error("Missing parent")
        }
    }

    private fun isSafeTag(node: Node?): Boolean {
        if (node == null || isMetaRefresh(node)) return false

        val tag = node.nodeName().lowercase()
        return tag in ALLOWED_TAGS
    }

    private fun isMetaRefresh(node: Node): Boolean {
        val tag = node.nodeName().lowercase()
        if (tag != "meta") return false

        val attributeValue = node.attributes().getIgnoreCase("http-equiv").trim().lowercase()
        return attributeValue == "refresh"
    }
}
