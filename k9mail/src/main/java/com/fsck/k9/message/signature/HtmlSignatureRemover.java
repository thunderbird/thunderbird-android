package com.fsck.k9.message.signature;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

import com.fsck.k9.K9;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;


public class HtmlSignatureRemover {
    private static final Pattern DASH_SIGNATURE_HTML = Pattern.compile("(<br( /)?>|\r?\n)-- <br( /)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_START = Pattern.compile("<blockquote", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCKQUOTE_END = Pattern.compile("</blockquote>", Pattern.CASE_INSENSITIVE);

    
    public static String stripSignature(String content) {
        Matcher dashSignatureHtml = DASH_SIGNATURE_HTML.matcher(content);
        if (dashSignatureHtml.find()) {
            Matcher blockquoteStart = BLOCKQUOTE_START.matcher(content);
            Matcher blockquoteEnd = BLOCKQUOTE_END.matcher(content);
            List<Integer> start = new ArrayList<>();
            List<Integer> end = new ArrayList<>();

            while (blockquoteStart.find()) {
                start.add(blockquoteStart.start());
            }
            while (blockquoteEnd.find()) {
                end.add(blockquoteEnd.start());
            }
            if (start.size() != end.size()) {
                Timber.d("There are %d <blockquote> tags, but %d </blockquote> tags. Refusing to strip.",
                        start.size(), end.size());
            } else if (start.size() > 0) {
                // Ignore quoted signatures in blockquotes.
                dashSignatureHtml.region(0, start.get(0));
                if (dashSignatureHtml.find()) {
                    // before first <blockquote>.
                    content = content.substring(0, dashSignatureHtml.start());
                } else {
                    for (int i = 0; i < start.size() - 1; i++) {
                        // within blockquotes.
                        if (end.get(i) < start.get(i + 1)) {
                            dashSignatureHtml.region(end.get(i), start.get(i + 1));
                            if (dashSignatureHtml.find()) {
                                content = content.substring(0, dashSignatureHtml.start());
                                break;
                            }
                        }
                    }
                    if (end.get(end.size() - 1) < content.length()) {
                        // after last </blockquote>.
                        dashSignatureHtml.region(end.get(end.size() - 1), content.length());
                        if (dashSignatureHtml.find()) {
                            content = content.substring(0, dashSignatureHtml.start());
                        }
                    }
                }
            } else {
                // No blockquotes found.
                content = content.substring(0, dashSignatureHtml.start());
            }
        }

        // Fix the stripping off of closing tags if a signature was stripped,
        // as well as clean up the HTML of the quoted message.
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties properties = cleaner.getProperties();

        // see http://htmlcleaner.sourceforge.net/parameters.php for descriptions
        properties.setNamespacesAware(false);
        properties.setAdvancedXmlEscape(false);
        properties.setOmitXmlDeclaration(true);
        properties.setOmitDoctypeDeclaration(false);
        properties.setTranslateSpecialEntities(false);
        properties.setRecognizeUnicodeChars(false);

        TagNode node = cleaner.clean(content);
        SimpleHtmlSerializer htmlSerialized = new SimpleHtmlSerializer(properties);
        content = htmlSerialized.getAsString(node, "UTF8");
        return content;
    }
}
