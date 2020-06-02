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
                .addTags("font", "hr", "ins", "del", "center", "map", "area")
                .addAttributes("font", "color", "face", "size")
                .addAttributes("table", "align", "background", "bgcolor", "border", "cellpadding", "cellspacing",
                        "width")
                .addAttributes("tr", "align", "background", "bgcolor", "valign")
                .addAttributes("th",
                        "align", "background", "bgcolor", "colspan", "headers", "height", "nowrap", "rowspan", "scope",
                        "sorted", "valign", "width")
                .addAttributes("td",
                        "align", "background", "bgcolor", "colspan", "headers", "height", "nowrap", "rowspan", "scope",
                        "valign", "width")
                .addAttributes("map", "name")
                .addAttributes("area", "shape", "coords", "href", "alt")
                .addProtocols("area", "href", "http", "https")
                .addAttributes("img", "usemap")
                .addAttributes(":all", "class", "style", "id", "dir")
                .addProtocols("img", "src", "http", "https", "cid", "data")
                .addProtocols("a", "href", "tel", "sip", "bitcoin", "ethereum", "rtsp");

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
