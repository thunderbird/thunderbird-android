package com.fsck.k9.helper;


import android.support.annotation.VisibleForTesting;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlSerializer;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;


public class HtmlSanitizer {
    private static final HtmlCleaner HTML_CLEANER;
    private static final HtmlSerializer HTML_SERIALIZER;

    static {
        CleanerProperties properties = createCleanerProperties();
        HTML_CLEANER = new HtmlCleaner(properties);
        HTML_SERIALIZER = new SimpleHtmlSerializer(properties);
    }


    public static HtmlSanitizer getInstance() {
        return new HtmlSanitizer();
    }

    @VisibleForTesting
    HtmlSanitizer() {}


    public String sanitize(String html) {
        TagNode rootNode = HTML_CLEANER.clean(html);

        removeMetaRefresh(rootNode);

        return HTML_SERIALIZER.getAsString(rootNode, "UTF8");
    }

    private static CleanerProperties createCleanerProperties() {
        CleanerProperties properties = new CleanerProperties();

        // See http://htmlcleaner.sourceforge.net/parameters.php for descriptions
        properties.setNamespacesAware(false);
        properties.setAdvancedXmlEscape(false);
        properties.setOmitXmlDeclaration(true);
        properties.setOmitDoctypeDeclaration(false);
        properties.setTranslateSpecialEntities(false);
        properties.setRecognizeUnicodeChars(false);

        return properties;
    }

    private void removeMetaRefresh(TagNode rootNode) {
        for (TagNode element : rootNode.getElementListByName("meta", true)) {
            String httpEquiv = element.getAttributeByName("http-equiv");
            if (httpEquiv != null && httpEquiv.trim().equalsIgnoreCase("refresh")) {
                element.removeFromTree();
            }
        }
    }
}
