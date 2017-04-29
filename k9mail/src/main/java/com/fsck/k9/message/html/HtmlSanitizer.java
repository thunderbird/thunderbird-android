package com.fsck.k9.message.html;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;


public class HtmlSanitizer {
    private final HeadCleaner headCleaner;
    private final Cleaner cleaner;

    HtmlSanitizer() {
        Whitelist whitelist = Whitelist.relaxed()
                .addTags("font")
                .addAttributes("table", "align", "bgcolor", "border", "cellpadding", "cellspacing", "width")
                .addAttributes(":all", "class", "style", "id")
                .addProtocols("img", "src", "http", "https", "cid", "data");

        cleaner = new Cleaner(whitelist);
        headCleaner = new HeadCleaner();
    }

    public Document sanitize(String html) {
        Document dirtyDocument = Jsoup.parse(html);
        Document cleanedDocument = cleaner.clean(dirtyDocument);
        headCleaner.clean(dirtyDocument, cleanedDocument);
        return cleanedDocument;
    }
}
