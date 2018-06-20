package com.fsck.k9.message.html;


import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.text.Annotation;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;

import com.fsck.k9.K9;
import org.xml.sax.XMLReader;

/**
 * Contains common routines to convert html to text and vice versa.
 */
public class HtmlConverter {
    /**
     * When generating previews, Spannable objects that can't be converted into a String are
     * represented as 0xfffc. When displayed, these show up as undisplayed squares. These constants
     * define the object character and the replacement character.
     */
    private static final char PREVIEW_OBJECT_CHARACTER = (char)0xfffc;
    private static final char PREVIEW_OBJECT_REPLACEMENT = (char)0x20;  // space

    /**
     * toHtml() converts non-breaking spaces into the UTF-8 non-breaking space, which doesn't get
     * rendered properly in some clients. Replace it with a simple space.
     */
    private static final char NBSP_CHARACTER = (char)0x00a0;    // utf-8 non-breaking space
    private static final char NBSP_REPLACEMENT = (char)0x20;    // space


    /**
     * Convert an HTML string to a plain text string.
     * @param html HTML string to convert.
     * @return Plain text result.
     */
    public static String htmlToText(final String html) {
        return Html.fromHtml(html, null, new HtmlToTextTagHandler()).toString()
               .replace(PREVIEW_OBJECT_CHARACTER, PREVIEW_OBJECT_REPLACEMENT)
               .replace(NBSP_CHARACTER, NBSP_REPLACEMENT);
    }

    /**
     * Custom tag handler to use when converting HTML messages to text. It currently handles text
     * representations of HTML tags that Android's built-in parser doesn't understand and hides code
     * contained in STYLE and SCRIPT blocks.
     */
    private static class HtmlToTextTagHandler implements Html.TagHandler {
        // List of tags whose content should be ignored.
        private static final Set<String> TAGS_WITH_IGNORED_CONTENT;
        static {
            Set<String> set = new HashSet<String>();
            set.add("style");
            set.add("script");
            set.add("title");
            set.add("!");   // comments
            TAGS_WITH_IGNORED_CONTENT = Collections.unmodifiableSet(set);
        }

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            tag = tag.toLowerCase(Locale.US);
            if (tag.equals("hr") && opening) {
                // In the case of an <hr>, replace it with a bunch of underscores. This is roughly
                // the behaviour of Outlook in Rich Text mode.
                output.append("_____________________________________________\r\n");
            } else if (TAGS_WITH_IGNORED_CONTENT.contains(tag)) {
                handleIgnoredTag(opening, output);
            }
        }

        private static final String IGNORED_ANNOTATION_KEY = "K9_ANNOTATION";
        private static final String IGNORED_ANNOTATION_VALUE = "hiddenSpan";

        /**
         * When we come upon an ignored tag, we mark it with an Annotation object with a specific key
         * and value as above. We don't really need to be checking these values since Html.fromHtml()
         * doesn't use Annotation spans, but we should do it now to be safe in case they do start using
         * it in the future.
         * @param opening If this is an opening tag or not.
         * @param output Spannable string that we're working with.
         */
        private void handleIgnoredTag(boolean opening, Editable output) {
            int len = output.length();
            if (opening) {
                output.setSpan(new Annotation(IGNORED_ANNOTATION_KEY, IGNORED_ANNOTATION_VALUE), len,
                               len, Spannable.SPAN_MARK_MARK);
            } else {
                Object start = getOpeningAnnotation(output);
                if (start != null) {
                    int where = output.getSpanStart(start);
                    // Remove the temporary Annotation span.
                    output.removeSpan(start);
                    // Delete everything between the start of the Annotation and the end of the string
                    // (what we've generated so far).
                    output.delete(where, len);
                }
            }
        }

        /**
         * Fetch the matching opening Annotation object and verify that it's the one added by K9.
         * @param output Spannable string we're working with.
         * @return Starting Annotation object.
         */
        private Object getOpeningAnnotation(Editable output) {
            Object[] objs = output.getSpans(0, output.length(), Annotation.class);
            for (int i = objs.length - 1; i >= 0; i--) {
                Annotation span = (Annotation) objs[i];
                if (output.getSpanFlags(objs[i]) == Spannable.SPAN_MARK_MARK
                        && span.getKey().equals(IGNORED_ANNOTATION_KEY)
                        && span.getValue().equals(IGNORED_ANNOTATION_VALUE)) {
                    return objs[i];
                }
            }
            return null;
        }
    }

    /**
     * Convert a text string into an HTML document.
     *
     * <p>
     * No HTML headers or footers are added to the result.  Headers and footers
     * are added at display time.
     * </p>
     */
    public static String textToHtml(String text) {
        return EmailTextToHtml.convert(text);
    }

    public static String wrapStatusMessage(CharSequence status) {
        return wrapMessageContent("<div style=\"text-align:center; color: grey;\">" + status + "</div>");
    }

    public static String wrapMessageContent(CharSequence messageContent) {
        // Include a meta tag so the WebView will not use a fixed viewport width of 980 px
        return "<html dir=\"auto\"><head><meta name=\"viewport\" content=\"width=device-width\"/>" +
                HtmlConverter.cssStyleTheme() +
                HtmlConverter.cssStylePre() +
                "</head><body>" +
                messageContent +
                "</body></html>";
    }

    static String cssStyleTheme() {
        if (K9.getK9MessageViewTheme() == K9.Theme.DARK)  {
            return "<style type=\"text/css\">" +
                    "* { background: black ! important; color: #F3F3F3 !important }" +
                    ":link, :link * { color: #CCFF33 !important }" +
                    ":visited, :visited * { color: #551A8B !important }</style> ";
        } else {
            return "";
        }
    }

    /**
     * Dynamically generate a CSS style for {@code <pre>} elements.
     *
     *  <p>
     *  The style incorporates the user's current preference
     *  setting for the font family used for plain text messages.
     *  </p>
     *
     * @return
     *      A {@code <style>} element that can be dynamically included in the HTML
     *      {@code <head>} element when messages are displayed.
     */
    static String cssStylePre() {
        final String font = K9.messageViewFixedWidthFont()
                ? "monospace"
                : "sans-serif";
        return "<style type=\"text/css\"> pre." + EmailTextToHtml.K9MAIL_CSS_CLASS +
                " {white-space: pre-wrap; word-wrap:break-word; " +
                "font-family: " + font + "; margin-top: 0px}</style>";
    }

    /**
     * Convert a plain text string into an HTML fragment.
     */
    public static String textToHtmlFragment(String text) {
        return TextToHtml.toHtmlFragment(text);
    }

    /**
     * Convert HTML to a {@link Spanned} that can be used in a {@link android.widget.TextView}.
     *
     * @param html
     *         The HTML fragment to be converted.
     *
     * @return A {@link Spanned} containing the text in {@code html} formatted using spans.
     */
    public static Spanned htmlToSpanned(String html) {
        return Html.fromHtml(html, null, new ListTagHandler());
    }

    /**
     * {@link TagHandler} that supports unordered lists.
     *
     * @see HtmlConverter#htmlToSpanned(String)
     */
    public static class ListTagHandler implements TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equals("ul")) {
                if (opening) {
                    char lastChar = 0;
                    if (output.length() > 0) {
                        lastChar = output.charAt(output.length() - 1);
                    }
                    if (lastChar != '\n') {
                        output.append("\r\n");
                    }
                } else {
                    output.append("\r\n");
                }
            }

            if (tag.equals("li")) {
                if (opening) {
                    output.append("\t•  ");
                } else {
                    output.append("\r\n");
                }
            }
        }
    }
}
