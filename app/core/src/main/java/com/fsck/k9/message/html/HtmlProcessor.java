package com.fsck.k9.message.html;


import org.jsoup.nodes.Document;


public class HtmlProcessor {
    private final HtmlSanitizer htmlSanitizer;
    private final DisplayHtml displayHtml;


    HtmlProcessor(HtmlSanitizer htmlSanitizer, DisplayHtml displayHtml) {
        this.htmlSanitizer = htmlSanitizer;
        this.displayHtml = displayHtml;
    }

    public String processForDisplay(String html) {
        Document document = htmlSanitizer.sanitize(html);
        addCustomHeadContents(document);

        return toCompactString(document);
    }

    private void addCustomHeadContents(Document document) {
        document.head().append("<meta name=\"viewport\" content=\"width=device-width\"/>" +
                displayHtml.cssStyleTheme() +
                displayHtml.cssStylePre());
    }

    public static String toCompactString(Document document) {
        document.outputSettings()
                .prettyPrint(false)
                .indentAmount(0);

        return document.html();
    }
}
