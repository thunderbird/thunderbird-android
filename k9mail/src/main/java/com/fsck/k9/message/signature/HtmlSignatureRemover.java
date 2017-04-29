package com.fsck.k9.message.signature;


import java.util.regex.Pattern;

import android.support.annotation.NonNull;

import com.fsck.k9.helper.jsoup.AdvancedNodeTraversor;
import com.fsck.k9.helper.jsoup.NodeFilter;
import com.fsck.k9.message.html.HtmlProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;


public class HtmlSignatureRemover {
    public static String stripSignature(String content) {
        return new HtmlSignatureRemover().stripSignatureInternal(content);
    }

    private String stripSignatureInternal(String content) {
        Document document = Jsoup.parse(content);

        AdvancedNodeTraversor nodeTraversor = new AdvancedNodeTraversor(new StripSignatureFilter());
        nodeTraversor.filter(document.body());

        return HtmlProcessor.toCompactString(document);
    }


    static class StripSignatureFilter implements NodeFilter {
        private static final Pattern DASH_SIGNATURE_HTML = Pattern.compile("\\s*-- \\s*", Pattern.CASE_INSENSITIVE);
        private static final Tag BLOCKQUOTE = Tag.valueOf("blockquote");
        private static final Tag BR = Tag.valueOf("br");
        private static final Tag P = Tag.valueOf("p");


        private boolean signatureFound = false;
        private boolean lastElementCausedLineBreak = false;
        private Element brElementPrecedingDashes;


        @NonNull
        @Override
        public HeadFilterDecision head(Node node, int depth) {
            if (signatureFound) {
                return HeadFilterDecision.REMOVE;
            }

            if (node instanceof Element) {
                lastElementCausedLineBreak = false;

                Element element = (Element) node;
                if (element.tag().equals(BLOCKQUOTE)) {
                    return HeadFilterDecision.SKIP_ENTIRELY;
                }
            } else if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                if (lastElementCausedLineBreak && DASH_SIGNATURE_HTML.matcher(textNode.getWholeText()).matches()) {
                    Node nextNode = node.nextSibling();
                    if (nextNode instanceof Element && ((Element) nextNode).tag().equals(BR)) {
                        signatureFound = true;
                        if (brElementPrecedingDashes != null) {
                            brElementPrecedingDashes.remove();
                            brElementPrecedingDashes = null;
                        }

                        return HeadFilterDecision.REMOVE;
                    }
                }
            }

            return HeadFilterDecision.CONTINUE;
        }

        @NonNull
        @Override
        public TailFilterDecision tail(Node node, int depth) {
            if (signatureFound) {
                return TailFilterDecision.CONTINUE;
            }

            if (node instanceof Element) {
                Element element = (Element) node;
                boolean elementIsBr = element.tag().equals(BR);
                if (elementIsBr || element.tag().equals(P)) {
                    lastElementCausedLineBreak = true;
                    brElementPrecedingDashes = elementIsBr ? element : null;
                    return TailFilterDecision.CONTINUE;
                }
            }

            lastElementCausedLineBreak = false;
            return TailFilterDecision.CONTINUE;
        }
    }
}
