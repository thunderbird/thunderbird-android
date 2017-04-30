package com.fsck.k9.message.html;


public class HtmlSanitizerHelper {
    public static HtmlSanitizer getDummyHtmlSanitizer() {
        return new HtmlSanitizer() {
            @Override
            public String sanitize(String html) {
                return html;
            }
        };
    }
}
