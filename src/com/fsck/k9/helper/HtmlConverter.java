package com.fsck.k9.helper;

import android.text.*;
import android.text.Html.TagHandler;
import android.util.Log;
import com.fsck.k9.K9;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

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
                output.append("_____________________________________________\n");
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

        StringReader reader = new StringReader(text);
        StringBuilder buff = new StringBuilder(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);

        buff.append(htmlifyMessageHeader());

        int c;
        try {
            while ((c = reader.read()) != -1) {
                switch (c) {
                case '\n':
                    // pine treats <br> as two newlines, but <br/> as one newline.  Use <br/> so our messages aren't
                    // doublespaced.
                    buff.append("<br />");
                    break;
                case '\r':
                    break;
                default:
                    buff.append((char)c);
                }//switch
            }
        } catch (IOException e) {
            //Should never happen
            Log.e(K9.LOG_TAG, "Could not read string to convert text to HTML:", e);
        }

        buff.append(htmlifyMessageFooter());

        return buff.toString();
    }

    private static final String HTML_BLOCKQUOTE_COLOR_TOKEN = "$$COLOR$$";
    private static final String HTML_BLOCKQUOTE_START = "<blockquote class=\"gmail_quote\" " +
            "style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid $$COLOR$$; padding-left: 1ex;\">";
    private static final String HTML_BLOCKQUOTE_END = "</blockquote>";
    private static final String HTML_NEWLINE = "<br />";

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
        StringReader reader = new StringReader(text);
        StringBuilder buff = new StringBuilder(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);
        boolean isStartOfLine = false;  // Are we currently at the start of a line?
        int quoteDepth = 0; // Number of DIVs deep we are.
        int quotesThisLine = 0; // How deep we should be quoting for this line.
        try {
            int c;
            while ((c = reader.read()) != -1) {
                switch (c) {
                case '\n':
                    // pine treats <br> as two newlines, but <br/> as one newline.  Use <br/> so our messages aren't
                    // doublespaced.
                    buff.append(HTML_NEWLINE);
                    isStartOfLine = true;
                    quotesThisLine = 0;
                    break;
                case '&':
                    buff.append("&amp;");
                    break;
                case '<':
                    buff.append("&lt;");
                    break;
                case '>':
                    if (isStartOfLine) {
                        quotesThisLine++;
                    } else {
                        // We use a token here which can't occur in htmlified text because &gt; is valid
                        // within links (where > is not), and linkifying links will include it if we
                        // do it here. We'll make another pass and change this back to &gt; after
                        // the linkification is done.
                        buff.append("<gt>");
                    }
                    break;
                case '\r':
                    break;
                case ' ':
                    if (isStartOfLine) {
                        // If we're still in the start of the line and we have spaces, don't output them, since they
                        // may be collapsed by our div-converting magic.
                        break;
                    }
                default:
                    if (isStartOfLine) {
                        // Not a quote character and not a space.  Content is starting now.
                        isStartOfLine = false;
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
                        quoteDepth = quotesThisLine;
                    }
                    buff.append((char)c);
                }//switch
            }
        } catch (IOException e) {
            //Should never happen
            Log.e(K9.LOG_TAG, "Could not read string to convert text to HTML:", e);
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

        // Replace lines of -,= or _ with horizontal rules
        text = text.replaceAll("\\s*([-=_]{30,}+)\\s*", "<hr />");

        // TODO: reverse engineer (or troll history) and document
        text = text.replaceAll("(?m)^([^\r\n]{4,}[\\s\\w,:;+/])(?:\r\n|\n|\r)(?=[a-z]\\S{0,10}[\\s\\n\\r])", "$1 ");

        // Compress four or more newlines down to two newlines
        text = text.replaceAll("(?m)(\r\n|\n|\r){4,}", "\n\n");

        StringBuffer sb = new StringBuffer(text.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);

        sb.append(htmlifyMessageHeader());
        linkifyText(text, sb);
        sb.append(htmlifyMessageFooter());

        text = sb.toString();

        // Above we replaced > with <gt>, now make it &gt;
        text = text.replaceAll("<gt>", "&gt;");

        return text;
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

    /**
     * Searches for link-like text in a string and turn it into a link. Append the result to
     * <tt>outputBuffer</tt>. <tt>text</tt> is not modified.
     * @param text Plain text to be linkified.
     * @param outputBuffer Buffer to append linked text to.
     */
    private static void linkifyText(final String text, final StringBuffer outputBuffer) {
        Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
        while (m.find()) {
            int start = m.start();
            if (start == 0 || (start != 0 && text.charAt(start - 1) != '@')) {
                if (m.group().indexOf(':') > 0) { // With no URI-schema we may get "http:/" links with the second / missing
                    m.appendReplacement(outputBuffer, "<a href=\"$0\">$0</a>");
                } else {
                    m.appendReplacement(outputBuffer, "<a href=\"http://$0\">$0</a>");
                }
            } else {
                m.appendReplacement(outputBuffer, "$0");
            }
        }

        m.appendTail(outputBuffer);
    }

    /*
     * Lightweight method to check whether the message contains emoji or not.
     * Useful to avoid calling the heavyweight convertEmoji2Img method.
     * We don't use String.codePointAt here for performance reasons.
     */
    private static boolean hasEmoji(String html) {
        for (int i = 0; i < html.length(); ++i) {
            char c = html.charAt(i);
            if (c >= 0xDBB8 && c < 0xDBBC)
                return true;
        }
        return false;
    }

    public static String convertEmoji2Img(String html) {
        if (!hasEmoji(html)) {
            return html;

        }
        StringBuilder buff = new StringBuilder(html.length() + 512);
        for (int i = 0; i < html.length(); i = html.offsetByCodePoints(i, 1)) {
            int codePoint = html.codePointAt(i);
            String emoji = getEmojiForCodePoint(codePoint);
            if (emoji != null)
                buff.append("<img src=\"file:///android_asset/emoticons/").append(emoji).append(".gif\" alt=\"").append(emoji).append("\" />");
            else
                buff.appendCodePoint(codePoint);

        }
        return buff.toString();
    }

    private static String getEmojiForCodePoint(int codePoint) {
        // Derived from http://code.google.com/p/emoji4unicode/source/browse/trunk/data/emoji4unicode.xml
        // XXX: This doesn't cover all the characters.  More emoticons are wanted.
        switch (codePoint) {
        case 0xFE000:
            return "sun";
        case 0xFE001:
            return "cloud";
        case 0xFE002:
            return "rain";
        case 0xFE003:
            return "snow";
        case 0xFE004:
            return "thunder";
        case 0xFE005:
            return "typhoon";
        case 0xFE006:
            return "mist";
        case 0xFE007:
            return "sprinkle";
        case 0xFE008:
            return "night";
        case 0xFE009:
            return "sun";
        case 0xFE00A:
            return "sun";
        case 0xFE00C:
            return "sun";
        case 0xFE010:
            return "night";
        case 0xFE011:
            return "newmoon";
        case 0xFE012:
            return "moon1";
        case 0xFE013:
            return "moon2";
        case 0xFE014:
            return "moon3";
        case 0xFE015:
            return "fullmoon";
        case 0xFE016:
            return "moon2";
        case 0xFE018:
            return "soon";
        case 0xFE019:
            return "on";
        case 0xFE01A:
            return "end";
        case 0xFE01B:
            return "sandclock";
        case 0xFE01C:
            return "sandclock";
        case 0xFE01D:
            return "watch";
        case 0xFE01E:
            return "clock";
        case 0xFE01F:
            return "clock";
        case 0xFE020:
            return "clock";
        case 0xFE021:
            return "clock";
        case 0xFE022:
            return "clock";
        case 0xFE023:
            return "clock";
        case 0xFE024:
            return "clock";
        case 0xFE025:
            return "clock";
        case 0xFE026:
            return "clock";
        case 0xFE027:
            return "clock";
        case 0xFE028:
            return "clock";
        case 0xFE029:
            return "clock";
        case 0xFE02A:
            return "clock";
        case 0xFE02B:
            return "aries";
        case 0xFE02C:
            return "taurus";
        case 0xFE02D:
            return "gemini";
        case 0xFE02E:
            return "cancer";
        case 0xFE02F:
            return "leo";
        case 0xFE030:
            return "virgo";
        case 0xFE031:
            return "libra";
        case 0xFE032:
            return "scorpius";
        case 0xFE033:
            return "sagittarius";
        case 0xFE034:
            return "capricornus";
        case 0xFE035:
            return "aquarius";
        case 0xFE036:
            return "pisces";
        case 0xFE038:
            return "wave";
        case 0xFE03B:
            return "night";
        case 0xFE03C:
            return "clover";
        case 0xFE03D:
            return "tulip";
        case 0xFE03E:
            return "bud";
        case 0xFE03F:
            return "maple";
        case 0xFE040:
            return "cherryblossom";
        case 0xFE042:
            return "maple";
        case 0xFE04E:
            return "clover";
        case 0xFE04F:
            return "cherry";
        case 0xFE050:
            return "banana";
        case 0xFE051:
            return "apple";
        case 0xFE05B:
            return "apple";
        case 0xFE190:
            return "eye";
        case 0xFE191:
            return "ear";
        case 0xFE193:
            return "kissmark";
        case 0xFE194:
            return "bleah";
        case 0xFE195:
            return "rouge";
        case 0xFE198:
            return "hairsalon";
        case 0xFE19A:
            return "shadow";
        case 0xFE19B:
            return "happy01";
        case 0xFE19C:
            return "happy01";
        case 0xFE19D:
            return "happy01";
        case 0xFE19E:
            return "happy01";
        case 0xFE1B7:
            return "dog";
        case 0xFE1B8:
            return "cat";
        case 0xFE1B9:
            return "snail";
        case 0xFE1BA:
            return "chick";
        case 0xFE1BB:
            return "chick";
        case 0xFE1BC:
            return "penguin";
        case 0xFE1BD:
            return "fish";
        case 0xFE1BE:
            return "horse";
        case 0xFE1BF:
            return "pig";
        case 0xFE1C8:
            return "chick";
        case 0xFE1C9:
            return "fish";
        case 0xFE1CF:
            return "aries";
        case 0xFE1D0:
            return "dog";
        case 0xFE1D8:
            return "dog";
        case 0xFE1D9:
            return "fish";
        case 0xFE1DB:
            return "foot";
        case 0xFE1DD:
            return "chick";
        case 0xFE1E0:
            return "pig";
        case 0xFE1E3:
            return "cancer";
        case 0xFE320:
            return "angry";
        case 0xFE321:
            return "sad";
        case 0xFE322:
            return "wobbly";
        case 0xFE323:
            return "despair";
        case 0xFE324:
            return "wobbly";
        case 0xFE325:
            return "coldsweats02";
        case 0xFE326:
            return "gawk";
        case 0xFE327:
            return "lovely";
        case 0xFE328:
            return "smile";
        case 0xFE329:
            return "bleah";
        case 0xFE32A:
            return "bleah";
        case 0xFE32B:
            return "delicious";
        case 0xFE32C:
            return "lovely";
        case 0xFE32D:
            return "lovely";
        case 0xFE32F:
            return "happy02";
        case 0xFE330:
            return "happy01";
        case 0xFE331:
            return "coldsweats01";
        case 0xFE332:
            return "happy02";
        case 0xFE333:
            return "smile";
        case 0xFE334:
            return "happy02";
        case 0xFE335:
            return "delicious";
        case 0xFE336:
            return "happy01";
        case 0xFE337:
            return "happy01";
        case 0xFE338:
            return "coldsweats01";
        case 0xFE339:
            return "weep";
        case 0xFE33A:
            return "crying";
        case 0xFE33B:
            return "shock";
        case 0xFE33C:
            return "bearing";
        case 0xFE33D:
            return "pout";
        case 0xFE33E:
            return "confident";
        case 0xFE33F:
            return "sad";
        case 0xFE340:
            return "think";
        case 0xFE341:
            return "shock";
        case 0xFE342:
            return "sleepy";
        case 0xFE343:
            return "catface";
        case 0xFE344:
            return "coldsweats02";
        case 0xFE345:
            return "coldsweats02";
        case 0xFE346:
            return "bearing";
        case 0xFE347:
            return "wink";
        case 0xFE348:
            return "happy01";
        case 0xFE349:
            return "smile";
        case 0xFE34A:
            return "happy02";
        case 0xFE34B:
            return "lovely";
        case 0xFE34C:
            return "lovely";
        case 0xFE34D:
            return "weep";
        case 0xFE34E:
            return "pout";
        case 0xFE34F:
            return "smile";
        case 0xFE350:
            return "sad";
        case 0xFE351:
            return "ng";
        case 0xFE352:
            return "ok";
        case 0xFE357:
            return "paper";
        case 0xFE359:
            return "sad";
        case 0xFE35A:
            return "angry";
        case 0xFE4B0:
            return "house";
        case 0xFE4B1:
            return "house";
        case 0xFE4B2:
            return "building";
        case 0xFE4B3:
            return "postoffice";
        case 0xFE4B4:
            return "hospital";
        case 0xFE4B5:
            return "bank";
        case 0xFE4B6:
            return "atm";
        case 0xFE4B7:
            return "hotel";
        case 0xFE4B9:
            return "24hours";
        case 0xFE4BA:
            return "school";
        case 0xFE4C1:
            return "ship";
        case 0xFE4C2:
            return "bottle";
        case 0xFE4C3:
            return "fuji";
        case 0xFE4C9:
            return "wrench";
        case 0xFE4CC:
            return "shoe";
        case 0xFE4CD:
            return "shoe";
        case 0xFE4CE:
            return "eyeglass";
        case 0xFE4CF:
            return "t-shirt";
        case 0xFE4D0:
            return "denim";
        case 0xFE4D1:
            return "crown";
        case 0xFE4D2:
            return "crown";
        case 0xFE4D6:
            return "boutique";
        case 0xFE4D7:
            return "boutique";
        case 0xFE4DB:
            return "t-shirt";
        case 0xFE4DC:
            return "moneybag";
        case 0xFE4DD:
            return "dollar";
        case 0xFE4E0:
            return "dollar";
        case 0xFE4E2:
            return "yen";
        case 0xFE4E3:
            return "dollar";
        case 0xFE4EF:
            return "camera";
        case 0xFE4F0:
            return "bag";
        case 0xFE4F1:
            return "pouch";
        case 0xFE4F2:
            return "bell";
        case 0xFE4F3:
            return "door";
        case 0xFE4F9:
            return "movie";
        case 0xFE4FB:
            return "flair";
        case 0xFE4FD:
            return "sign05";
        case 0xFE4FF:
            return "book";
        case 0xFE500:
            return "book";
        case 0xFE501:
            return "book";
        case 0xFE502:
            return "book";
        case 0xFE503:
            return "book";
        case 0xFE505:
            return "spa";
        case 0xFE506:
            return "toilet";
        case 0xFE507:
            return "toilet";
        case 0xFE508:
            return "toilet";
        case 0xFE50F:
            return "ribbon";
        case 0xFE510:
            return "present";
        case 0xFE511:
            return "birthday";
        case 0xFE512:
            return "xmas";
        case 0xFE522:
            return "pocketbell";
        case 0xFE523:
            return "telephone";
        case 0xFE524:
            return "telephone";
        case 0xFE525:
            return "mobilephone";
        case 0xFE526:
            return "phoneto";
        case 0xFE527:
            return "memo";
        case 0xFE528:
            return "faxto";
        case 0xFE529:
            return "mail";
        case 0xFE52A:
            return "mailto";
        case 0xFE52B:
            return "mailto";
        case 0xFE52C:
            return "postoffice";
        case 0xFE52D:
            return "postoffice";
        case 0xFE52E:
            return "postoffice";
        case 0xFE535:
            return "present";
        case 0xFE536:
            return "pen";
        case 0xFE537:
            return "chair";
        case 0xFE538:
            return "pc";
        case 0xFE539:
            return "pencil";
        case 0xFE53A:
            return "clip";
        case 0xFE53B:
            return "bag";
        case 0xFE53E:
            return "hairsalon";
        case 0xFE540:
            return "memo";
        case 0xFE541:
            return "memo";
        case 0xFE545:
            return "book";
        case 0xFE546:
            return "book";
        case 0xFE547:
            return "book";
        case 0xFE548:
            return "memo";
        case 0xFE54D:
            return "book";
        case 0xFE54F:
            return "book";
        case 0xFE552:
            return "memo";
        case 0xFE553:
            return "foot";
        case 0xFE7D0:
            return "sports";
        case 0xFE7D1:
            return "baseball";
        case 0xFE7D2:
            return "golf";
        case 0xFE7D3:
            return "tennis";
        case 0xFE7D4:
            return "soccer";
        case 0xFE7D5:
            return "ski";
        case 0xFE7D6:
            return "basketball";
        case 0xFE7D7:
            return "motorsports";
        case 0xFE7D8:
            return "snowboard";
        case 0xFE7D9:
            return "run";
        case 0xFE7DA:
            return "snowboard";
        case 0xFE7DC:
            return "horse";
        case 0xFE7DF:
            return "train";
        case 0xFE7E0:
            return "subway";
        case 0xFE7E1:
            return "subway";
        case 0xFE7E2:
            return "bullettrain";
        case 0xFE7E3:
            return "bullettrain";
        case 0xFE7E4:
            return "car";
        case 0xFE7E5:
            return "rvcar";
        case 0xFE7E6:
            return "bus";
        case 0xFE7E8:
            return "ship";
        case 0xFE7E9:
            return "airplane";
        case 0xFE7EA:
            return "yacht";
        case 0xFE7EB:
            return "bicycle";
        case 0xFE7EE:
            return "yacht";
        case 0xFE7EF:
            return "car";
        case 0xFE7F0:
            return "run";
        case 0xFE7F5:
            return "gasstation";
        case 0xFE7F6:
            return "parking";
        case 0xFE7F7:
            return "signaler";
        case 0xFE7FA:
            return "spa";
        case 0xFE7FC:
            return "carouselpony";
        case 0xFE7FF:
            return "fish";
        case 0xFE800:
            return "karaoke";
        case 0xFE801:
            return "movie";
        case 0xFE802:
            return "movie";
        case 0xFE803:
            return "music";
        case 0xFE804:
            return "art";
        case 0xFE805:
            return "drama";
        case 0xFE806:
            return "event";
        case 0xFE807:
            return "ticket";
        case 0xFE808:
            return "slate";
        case 0xFE809:
            return "drama";
        case 0xFE80A:
            return "game";
        case 0xFE813:
            return "note";
        case 0xFE814:
            return "notes";
        case 0xFE81A:
            return "notes";
        case 0xFE81C:
            return "tv";
        case 0xFE81D:
            return "cd";
        case 0xFE81E:
            return "cd";
        case 0xFE823:
            return "kissmark";
        case 0xFE824:
            return "loveletter";
        case 0xFE825:
            return "ring";
        case 0xFE826:
            return "ring";
        case 0xFE827:
            return "kissmark";
        case 0xFE829:
            return "heart02";
        case 0xFE82B:
            return "freedial";
        case 0xFE82C:
            return "sharp";
        case 0xFE82D:
            return "mobaq";
        case 0xFE82E:
            return "one";
        case 0xFE82F:
            return "two";
        case 0xFE830:
            return "three";
        case 0xFE831:
            return "four";
        case 0xFE832:
            return "five";
        case 0xFE833:
            return "six";
        case 0xFE834:
            return "seven";
        case 0xFE835:
            return "eight";
        case 0xFE836:
            return "nine";
        case 0xFE837:
            return "zero";
        case 0xFE960:
            return "fastfood";
        case 0xFE961:
            return "riceball";
        case 0xFE962:
            return "cake";
        case 0xFE963:
            return "noodle";
        case 0xFE964:
            return "bread";
        case 0xFE96A:
            return "noodle";
        case 0xFE973:
            return "typhoon";
        case 0xFE980:
            return "restaurant";
        case 0xFE981:
            return "cafe";
        case 0xFE982:
            return "bar";
        case 0xFE983:
            return "beer";
        case 0xFE984:
            return "japanesetea";
        case 0xFE985:
            return "bottle";
        case 0xFE986:
            return "wine";
        case 0xFE987:
            return "beer";
        case 0xFE988:
            return "bar";
        case 0xFEAF0:
            return "upwardright";
        case 0xFEAF1:
            return "downwardright";
        case 0xFEAF2:
            return "upwardleft";
        case 0xFEAF3:
            return "downwardleft";
        case 0xFEAF4:
            return "up";
        case 0xFEAF5:
            return "down";
        case 0xFEAF6:
            return "leftright";
        case 0xFEAF7:
            return "updown";
        case 0xFEB04:
            return "sign01";
        case 0xFEB05:
            return "sign02";
        case 0xFEB06:
            return "sign03";
        case 0xFEB07:
            return "sign04";
        case 0xFEB08:
            return "sign05";
        case 0xFEB0B:
            return "sign01";
        case 0xFEB0C:
            return "heart01";
        case 0xFEB0D:
            return "heart02";
        case 0xFEB0E:
            return "heart03";
        case 0xFEB0F:
            return "heart04";
        case 0xFEB10:
            return "heart01";
        case 0xFEB11:
            return "heart02";
        case 0xFEB12:
            return "heart01";
        case 0xFEB13:
            return "heart01";
        case 0xFEB14:
            return "heart01";
        case 0xFEB15:
            return "heart01";
        case 0xFEB16:
            return "heart01";
        case 0xFEB17:
            return "heart01";
        case 0xFEB18:
            return "heart02";
        case 0xFEB19:
            return "cute";
        case 0xFEB1A:
            return "heart";
        case 0xFEB1B:
            return "spade";
        case 0xFEB1C:
            return "diamond";
        case 0xFEB1D:
            return "club";
        case 0xFEB1E:
            return "smoking";
        case 0xFEB1F:
            return "nosmoking";
        case 0xFEB20:
            return "wheelchair";
        case 0xFEB21:
            return "free";
        case 0xFEB22:
            return "flag";
        case 0xFEB23:
            return "danger";
        case 0xFEB26:
            return "ng";
        case 0xFEB27:
            return "ok";
        case 0xFEB28:
            return "ng";
        case 0xFEB29:
            return "copyright";
        case 0xFEB2A:
            return "tm";
        case 0xFEB2B:
            return "secret";
        case 0xFEB2C:
            return "recycle";
        case 0xFEB2D:
            return "r-mark";
        case 0xFEB2E:
            return "ban";
        case 0xFEB2F:
            return "empty";
        case 0xFEB30:
            return "pass";
        case 0xFEB31:
            return "full";
        case 0xFEB36:
            return "new";
        case 0xFEB44:
            return "fullmoon";
        case 0xFEB48:
            return "ban";
        case 0xFEB55:
            return "cute";
        case 0xFEB56:
            return "flair";
        case 0xFEB57:
            return "annoy";
        case 0xFEB58:
            return "bomb";
        case 0xFEB59:
            return "sleepy";
        case 0xFEB5A:
            return "impact";
        case 0xFEB5B:
            return "sweat01";
        case 0xFEB5C:
            return "sweat02";
        case 0xFEB5D:
            return "dash";
        case 0xFEB5F:
            return "sad";
        case 0xFEB60:
            return "shine";
        case 0xFEB61:
            return "cute";
        case 0xFEB62:
            return "cute";
        case 0xFEB63:
            return "newmoon";
        case 0xFEB64:
            return "newmoon";
        case 0xFEB65:
            return "newmoon";
        case 0xFEB66:
            return "newmoon";
        case 0xFEB67:
            return "newmoon";
        case 0xFEB77:
            return "shine";
        case 0xFEB81:
            return "id";
        case 0xFEB82:
            return "key";
        case 0xFEB83:
            return "enter";
        case 0xFEB84:
            return "clear";
        case 0xFEB85:
            return "search";
        case 0xFEB86:
            return "key";
        case 0xFEB87:
            return "key";
        case 0xFEB8A:
            return "key";
        case 0xFEB8D:
            return "search";
        case 0xFEB90:
            return "key";
        case 0xFEB91:
            return "recycle";
        case 0xFEB92:
            return "mail";
        case 0xFEB93:
            return "rock";
        case 0xFEB94:
            return "scissors";
        case 0xFEB95:
            return "paper";
        case 0xFEB96:
            return "punch";
        case 0xFEB97:
            return "good";
        case 0xFEB9D:
            return "paper";
        case 0xFEB9F:
            return "ok";
        case 0xFEBA0:
            return "down";
        case 0xFEBA1:
            return "paper";
        case 0xFEE10:
            return "info01";
        case 0xFEE11:
            return "info02";
        case 0xFEE12:
            return "by-d";
        case 0xFEE13:
            return "d-point";
        case 0xFEE14:
            return "appli01";
        case 0xFEE15:
            return "appli02";
        case 0xFEE1C:
            return "movie";
        default:
            return null;
        }
    }

    private static String htmlifyMessageHeader() {
        return "<pre class=\"" + K9MAIL_CSS_CLASS + "\">";
    }

    private static String htmlifyMessageFooter() {
        return "</pre>";
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
    public static String cssStylePre() {
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
        linkifyText(htmlified, linkified);

        // Add newlines and unescaping.
        //
        // For some reason, TextUtils.htmlEncode escapes ' into &apos;, which is technically part of the XHTML 1.0
        // standard, but Gmail doesn't recognize it as an HTML entity. We unescape that here.
        return linkified.toString().replace("\n", "<br>\n").replace("&apos;", "&#39;");
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
                        output.append("\n");
                    }
                } else {
                    output.append("\n");
                }
            }

            if (tag.equals("li")) {
                if (opening) {
                    output.append("\t•  ");
                } else {
                    output.append("\n");
                }
            }
        }
    }
}
