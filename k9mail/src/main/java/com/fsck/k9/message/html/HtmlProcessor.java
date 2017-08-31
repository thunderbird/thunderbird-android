package com.fsck.k9.message.html;


import org.jsoup.nodes.Document;


public class HtmlProcessor {
    private final HtmlSanitizer htmlSanitizer;


    public static HtmlProcessor newInstance() {
        HtmlSanitizer htmlSanitizer = new HtmlSanitizer();
        return new HtmlProcessor(htmlSanitizer);
    }

    private HtmlProcessor(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    public String processForDisplay(String html) {
        Document document = htmlSanitizer.sanitize(html);
        addCustomHeadContents(document);

        return toCompactString(document);
    }

    private void addCustomHeadContents(Document document) {
        document.head().append("<meta name=\"viewport\" content=\"width=device-width\"/>" +
                HtmlConverter.cssStyleTheme() +
                HtmlConverter.cssStylePre());
    }

    public static String toCompactString(Document document) {
        document.outputSettings()
                .prettyPrint(false)
                .indentAmount(0);

        return document.html();
    }
}
