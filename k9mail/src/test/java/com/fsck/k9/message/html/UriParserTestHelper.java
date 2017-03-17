package com.fsck.k9.message.html;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class UriParserTestHelper {
    public static void assertContainsLink(String expected, StringBuffer actual) {
        String linkifiedUri = actual.toString();
        Document document = Jsoup.parseBodyFragment(linkifiedUri);
        Element anchorElement = document.select("a").first();
        assertNotNull("No <a> element found", anchorElement);
        assertEquals(expected, anchorElement.text());
        assertEquals(expected, anchorElement.attr("href"));
    }

    public static void assertLinkOnly(String expected, StringBuffer actual) {
        String linkifiedUri = actual.toString();
        Document document = Jsoup.parseBodyFragment(linkifiedUri);
        Element anchorElement = document.select("a").first();
        assertNotNull("No <a> element found", anchorElement);
        assertEquals(expected, anchorElement.text());
        assertEquals(expected, anchorElement.attr("href"));

        assertAnchorElementIsSoleContent(document, anchorElement);
    }

    private static void assertAnchorElementIsSoleContent(Document document, Element anchorElement) {
        assertEquals(document.body(), anchorElement.parent());
        assertTrue("<a> element is surrounded by text", document.body().textNodes().isEmpty());
    }
}
