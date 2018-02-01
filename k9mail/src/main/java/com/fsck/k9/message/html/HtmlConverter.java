package com.fsck.k9.message.html;


import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import android.text.Annotation;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;

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

    // Number of extra bytes to allocate in a string buffer for htmlification.
    private static final int TEXT_TO_HTML_EXTRA_BUFFER_LENGTH = 512;

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

    private static final int MAX_SMART_HTMLIFY_MESSAGE_LENGTH = 1024 * 256 ;

    /**
     * Naively convert a text string into an HTML document.
     *
     * <p>
     * This method avoids using regular expressions on the entire message body to save memory.
     * </p>
     * <p>
     * No HTML headers or footers are added to the result.  Headers and footers
     * are added at display time in
     * {@link com.fsck.k9.view#MessageWebView.setText(String) MessageWebView.setText()}
     * </p>
     *
     * @param text
     *         Plain text string.
     * @return HTML string.
     */
    private static String simpleTextToHtml(String text) {
        // Encode HTML entities to make sure we don't display something evil.
        text = TextUtils.htmlEncode(text);

        StringBuilder buff = new StringBuilder(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);

        buff.append(htmlifyMessageHeader());

        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            switch (c) {
            case '\n':
                // pine treats <br> as two newlines, but <br/> as one newline.  Use <br/> so our messages aren't
                // doublespaced.
                buff.append("<br />");
                break;
            case '\r':
                break;
            default:
                buff.append(c);
            }//switch
        }

        buff.append(htmlifyMessageFooter());

        return buff.toString();
    }

    private static final String HTML_BLOCKQUOTE_COLOR_TOKEN = "$$COLOR$$";
    private static final String HTML_BLOCKQUOTE_START = "<blockquote class=\"gmail_quote\" " +
            "style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid $$COLOR$$; padding-left: 1ex;\">";
    private static final String HTML_BLOCKQUOTE_END = "</blockquote>";
    private static final String HTML_NEWLINE = "<br />";
    private static final Pattern ASCII_PATTERN_FOR_HR = Pattern.compile(
            "(^|\\Q" + HTML_NEWLINE + "\\E)\\s*((\\Q" + HTML_NEWLINE + "\\E)*" +
            "((((\\Q" + HTML_NEWLINE + "\\E){0,2}([-=_]{3,})(\\Q" + HTML_NEWLINE +
            "\\E){0,2})|(([-=_]{2,} ?)(8&lt;|<gt>8|%&lt;|<gt>%)" +
            "( ?[-=_]{2,})))+(\\Q" + HTML_NEWLINE + "\\E|$)))");

    /**
     * Convert a text string into an HTML document.
     *
     * <p>
     * Attempts to do smart replacement for large documents to prevent OOM
     * errors.
     * <p>
     * No HTML headers or footers are added to the result.  Headers and footers
     * are added at display time in
     * {@link com.fsck.k9.view#MessageWebView.setText(String) MessageWebView.setText()}
     * </p>
     * <p>
     * To convert to a fragment, use {@link #textToHtmlFragment(String)} .
     * </p>
     *
     * @param text
     *         Plain text string.
     * @return HTML string.
     */
    public static String textToHtml(String text) {
        // Our HTMLification code is somewhat memory intensive
        // and was causing lots of OOM errors on the market
        // if the message is big and plain text, just do
        // a trivial htmlification
        if (text.length() > MAX_SMART_HTMLIFY_MESSAGE_LENGTH) {
            return simpleTextToHtml(text);
        }
        StringBuilder buff = new StringBuilder(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);
        boolean isStartOfLine = true;  // Are we currently at the start of a line?
        int spaces = 0;
        int quoteDepth = 0; // Number of DIVs deep we are.
        int quotesThisLine = 0; // How deep we should be quoting for this line.
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (isStartOfLine) {
                switch (c) {
                case ' ':
                    spaces++;
                    break;
                case '>':
                    quotesThisLine++;
                    spaces = 0;
                    break;
                case '\n':
                    appendbq(buff, quotesThisLine, quoteDepth);
                    quoteDepth = quotesThisLine;

                    appendsp(buff, spaces);
                    spaces = 0;

                    appendchar(buff, c);
                    isStartOfLine = true;
                    quotesThisLine = 0;
                    break;
                default:
                    isStartOfLine = false;

                    appendbq(buff, quotesThisLine, quoteDepth);
                    quoteDepth = quotesThisLine;

                    appendsp(buff, spaces);
                    spaces = 0;

                    appendchar(buff, c);
                    isStartOfLine = false;
                    break;
                }
            }
            else {
                appendchar(buff, c);
                if (c == '\n') {
                    isStartOfLine = true;
                    quotesThisLine = 0;
                }
            }
        }
        // Close off any quotes we may have opened.
        if (quoteDepth > 0) {
            for (int i = quoteDepth; i > 0; i--) {
                buff.append(HTML_BLOCKQUOTE_END);
            }
        }
        text = buff.toString();

        // Make newlines at the end of blockquotes nicer by putting newlines beyond the first one outside of the
        // blockquote.
        text = text.replaceAll(
                   "\\Q" + HTML_NEWLINE + "\\E((\\Q" + HTML_NEWLINE + "\\E)+?)\\Q" + HTML_BLOCKQUOTE_END + "\\E",
                   HTML_BLOCKQUOTE_END + "$1"
               );

        text = ASCII_PATTERN_FOR_HR.matcher(text).replaceAll("<hr>");

        StringBuffer sb = new StringBuffer(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);

        sb.append(htmlifyMessageHeader());
        UriLinkifier.linkifyText(text, sb);
        sb.append(htmlifyMessageFooter());

        text = sb.toString();

        // Above we replaced > with <gt>, now make it &gt;
        text = text.replaceAll("<gt>", "&gt;");

        return text;
    }

    private static void appendchar(StringBuilder buff, int c) {
        switch (c) {
        case '&':
            buff.append("&amp;");
            break;
        case '<':
            buff.append("&lt;");
            break;
        case '>':
            // We use a token here which can't occur in htmlified text because &gt; is valid
            // within links (where > is not), and linkifying links will include it if we
            // do it here. We'll make another pass and change this back to &gt; after
            // the linkification is done.
            buff.append("<gt>");
            break;
        case '\r':
            break;
        case '\n':
            // pine treats <br> as two newlines, but <br/> as one newline.  Use <br/> so our messages aren't
            // doublespaced.
            buff.append(HTML_NEWLINE);
            break;
        default:
            buff.append((char)c);
            break;
        }
    }

    private static void appendsp(StringBuilder buff, int spaces) {
        while (spaces > 0) {
            buff.append(' ');
            spaces--;
        }
    }

    private static void appendbq(StringBuilder buff, int quotesThisLine, int quoteDepth) {
        // Add/remove blockquotes by comparing this line's quotes to the previous line's quotes.
        if (quotesThisLine > quoteDepth) {
            for (int i = quoteDepth; i < quotesThisLine; i++) {
                buff.append(HTML_BLOCKQUOTE_START.replace(HTML_BLOCKQUOTE_COLOR_TOKEN, getQuoteColor(i + 1)));
            }
        } else if (quotesThisLine < quoteDepth) {
            for (int i = quoteDepth; i > quotesThisLine; i--) {
                buff.append(HTML_BLOCKQUOTE_END);
            }
        }
    }

    protected static final String QUOTE_COLOR_DEFAULT = "#ccc";
    protected static final String QUOTE_COLOR_LEVEL_1 = "#729fcf";
    protected static final String QUOTE_COLOR_LEVEL_2 = "#ad7fa8";
    protected static final String QUOTE_COLOR_LEVEL_3 = "#8ae234";
    protected static final String QUOTE_COLOR_LEVEL_4 = "#fcaf3e";
    protected static final String QUOTE_COLOR_LEVEL_5 = "#e9b96e";
    private static final String K9MAIL_CSS_CLASS = "k9mail";

    /**
     * Return an HTML hex color string for a given quote level.
     * @param level Quote level
     * @return Hex color string with prepended #.
     */
    protected static String getQuoteColor(final int level) {
        switch(level) {
            case 1:
                return QUOTE_COLOR_LEVEL_1;
            case 2:
                return QUOTE_COLOR_LEVEL_2;
            case 3:
                return QUOTE_COLOR_LEVEL_3;
            case 4:
                return QUOTE_COLOR_LEVEL_4;
            case 5:
                return QUOTE_COLOR_LEVEL_5;
            default:
                return QUOTE_COLOR_DEFAULT;
        }
    }

    private static String htmlifyMessageHeader() {
        return "<pre class=\"" + K9MAIL_CSS_CLASS + "\">";
    }

    private static String htmlifyMessageFooter() {
        return "</pre>";
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
        return "<style type=\"text/css\"> pre." + K9MAIL_CSS_CLASS +
                " {white-space: pre-wrap; word-wrap:break-word; " +
                "font-family: " + font + "; margin-top: 0px}</style>";
    }

    /**
     * Convert a plain text string into an HTML fragment.
     * @param text Plain text.
     * @return HTML fragment.
     */
    public static String textToHtmlFragment(final String text) {
        // Escape the entities and add newlines.
        String htmlified = TextUtils.htmlEncode(text);

        // Linkify the message.
        StringBuffer linkified = new StringBuffer(htmlified.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);
        UriLinkifier.linkifyText(htmlified, linkified);

        // Add newlines and unescaping.
        //
        // For some reason, TextUtils.htmlEncode escapes ' into &apos;, which is technically part of the XHTML 1.0
        // standard, but Gmail doesn't recognize it as an HTML entity. We unescape that here.
        return linkified.toString().replaceAll("\r?\n", "<br>\r\n").replace("&apos;", "&#39;");
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
