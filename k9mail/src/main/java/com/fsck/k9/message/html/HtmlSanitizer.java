package com.fsck.k9.message.html;


import java.util.ListIterator;

import android.support.annotation.VisibleForTesting;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;


public class HtmlSanitizer {

    public static HtmlSanitizer getInstance() {
        return new HtmlSanitizer();
    }

    @VisibleForTesting
    HtmlSanitizer() {}


    public String sanitize(String html) {
        Document doc = Jsoup.parse( html );
        doc.outputSettings().prettyPrint(false);
        doc.outputSettings().charset("UTF-8");
        Whitelist whitelist = Whitelist.relaxed();
        whitelist.addTags("style");
        whitelist.addAttributes(":all", "class", "style");
        String bodyText = Jsoup.clean( doc.body().html(), whitelist );
        Document fullHtml = Parser.parse(html, "");
        fullHtml.outputSettings().prettyPrint(false);
        Document bodyDoc = Parser.parseBodyFragment(bodyText, "");
        bodyDoc.outputSettings().prettyPrint(false);
        fullHtml.body().replaceWith(bodyDoc.body());
        ListIterator<Element> metaTagIterator = fullHtml.getElementsByTag("meta").listIterator();
        while (metaTagIterator.hasNext()) {
            Element metaTag = metaTagIterator.next();
            if (metaTag.hasAttr("http-equiv") && metaTag.attr("http-equiv").trim().equalsIgnoreCase("refresh")) {
                metaTag.remove();
            }
        }
        return fullHtml.html();
    }
}
