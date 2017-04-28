package com.fsck.k9.message.html;


public class HtmlProcessor {
    private final HtmlSanitizer htmlSanitizer;


    public static HtmlProcessor newInstance() {
        HtmlSanitizer htmlSanitizer = HtmlSanitizer.getInstance();
        return new HtmlProcessor(htmlSanitizer);
    }

    private HtmlProcessor(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    public String processForDisplay(String html) {
        String wrappedHtml = HtmlConverter.wrapMessageContent(html);
        return htmlSanitizer.sanitize(wrappedHtml);
    }
}
