package com.fsck.k9.message.html;


import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spanned;

import com.fsck.k9.K9;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        Document document = Jsoup.parse(html);
        return HtmlToPlainText.toPlainText(document.body())
               .replace(PREVIEW_OBJECT_CHARACTER, PREVIEW_OBJECT_REPLACEMENT)
               .replace(NBSP_CHARACTER, NBSP_REPLACEMENT);
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
