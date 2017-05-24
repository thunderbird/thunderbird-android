package com.fsck.k9.message.html;


import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import static java.util.Arrays.asList;


class HeadCleaner {
    private static final List<String> ALLOWED_TAGS = asList("style", "meta");


    public void clean(Document dirtyDocument, Document cleanedDocument) {
        copySafeNodes(dirtyDocument.head(), cleanedDocument.head());
    }

    private void copySafeNodes(Element source, Element destination) {
        CleaningVisitor cleaningVisitor = new CleaningVisitor(source, destination);
        NodeTraversor traversor = new NodeTraversor(cleaningVisitor);
        traversor.traverse(source);
    }


    static class CleaningVisitor implements NodeVisitor {
        private final Element root;
        private Element destination;
        private boolean skipChildren = false;


        CleaningVisitor(Element root, Element destination) {
            this.root = root;
            this.destination = destination;
        }

        public void head(Node source, int depth) {
            if (skipChildren) {
                return;
            }

            if (source instanceof Element) {
                Element sourceElement = (Element) source;

                if (isSafeTag(sourceElement)) {
                    String sourceTag = sourceElement.tagName();
                    Attributes destinationAttributes = sourceElement.attributes().clone();
                    Element destinationChild = new Element(Tag.valueOf(sourceTag), sourceElement.baseUri(), destinationAttributes);

                    destination.appendChild(destinationChild);
                    destination = destinationChild;
                } else if (source != root) {
                    skipChildren = true;
                }
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode) source;
                TextNode destinationText = new TextNode(sourceText.getWholeText(), source.baseUri());
                destination.appendChild(destinationText);
            } else if (source instanceof DataNode && isSafeTag(source.parent())) {
                DataNode sourceData = (DataNode) source;
                DataNode destinationData = new DataNode(sourceData.getWholeData(), source.baseUri());
                destination.appendChild(destinationData);
            }
        }

        public void tail(Node source, int depth) {
            if (source == destination) {
                destination = destination.parent();
                skipChildren = false;
            }
        }

        private boolean isSafeTag(Node node) {
            if (isMetaRefresh(node)) {
                return false;
            }

            String tag = node.nodeName().toLowerCase(Locale.ROOT);
            return ALLOWED_TAGS.contains(tag);
        }

        private boolean isMetaRefresh(Node node) {
            if (!"meta".equalsIgnoreCase(node.nodeName())) {
                return false;
            }

            String attributeValue = node.attributes().getIgnoreCase("http-equiv");
            return "refresh".equalsIgnoreCase(attributeValue.trim());
        }
    }
}
