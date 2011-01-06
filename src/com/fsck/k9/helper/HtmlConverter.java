package com.fsck.k9.helper;

import android.text.Annotation;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import com.fsck.k9.K9;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Contains common routines to convert html to text and vice versa.
 */
public class HtmlConverter
{
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
    public static String htmlToText(final String html)
    {
        return Html.fromHtml(html, null, new HtmlToTextTagHandler()).toString()
               .replace(PREVIEW_OBJECT_CHARACTER, PREVIEW_OBJECT_REPLACEMENT)
               .replace(NBSP_CHARACTER, NBSP_REPLACEMENT);
    }

    /**
     * Custom tag handler to use when converting HTML messages to text. It currently handles text
     * representations of HTML tags that Android's built-in parser doesn't understand and hides code
     * contained in STYLE and SCRIPT blocks.
     */
    private static class HtmlToTextTagHandler implements Html.TagHandler
    {
        // List of tags whose content should be ignored.
        private static final Set<String> TAGS_WITH_IGNORED_CONTENT = Collections.unmodifiableSet(new HashSet<String>()
        {
            {
                add("style");
                add("script");
                add("title");
                add("!");   // comments
            }
        });

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader)
        {
            tag = tag.toLowerCase();
            if (tag.equals("hr") && opening)
            {
                // In the case of an <hr>, replace it with a bunch of underscores. This is roughly
                // the behaviour of Outlook in Rich Text mode.
                output.append("_____________________________________________\n");
            }
            else if (TAGS_WITH_IGNORED_CONTENT.contains(tag))
            {
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
        private void handleIgnoredTag(boolean opening, Editable output)
        {
            int len = output.length();
            if (opening)
            {
                output.setSpan(new Annotation(IGNORED_ANNOTATION_KEY, IGNORED_ANNOTATION_VALUE), len,
                               len, Spannable.SPAN_MARK_MARK);
            }
            else
            {
                Object start = getOpeningAnnotation(output);
                if (start != null)
                {
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
        private Object getOpeningAnnotation(Editable output)
        {
            Object[] objs = output.getSpans(0, output.length(), Annotation.class);
            for (int i = objs.length - 1; i >= 0; i--)
            {
                Annotation span = (Annotation) objs[i];
                if (output.getSpanFlags(objs[i]) == Spannable.SPAN_MARK_MARK
                        && span.getKey().equals(IGNORED_ANNOTATION_KEY)
                        && span.getValue().equals(IGNORED_ANNOTATION_VALUE))
                {
                    return objs[i];
                }
            }
            return null;
        }
    }

    private static final int MAX_SMART_HTMLIFY_MESSAGE_LENGTH = 1024 * 256 ;

    /**
     * Convert a text string into an HTML document. Attempts to do smart replacement for large
     * documents to prevent OOM errors.
     * @param text Plain text string.
     * @return HTML string.
     */
    public static String textToHtml(String text)
    {
        // Our HTMLification code is somewhat memory intensive
        // and was causing lots of OOM errors on the market
        // if the message is big and plain text, just do
        // a trivial htmlification
        if (text.length() > MAX_SMART_HTMLIFY_MESSAGE_LENGTH)
        {
            return "<html><head/><body>" +
                   htmlifyMessageHeader() +
                   text +
                   htmlifyMessageFooter() +
                   "</body></html>";
        }
        StringReader reader = new StringReader(text);
        StringBuilder buff = new StringBuilder(text.length() + 512);
        int c;
        try
        {
            while ((c = reader.read()) != -1)
            {
                switch (c)
                {
                    case '&':
                        buff.append("&amp;");
                        break;
                    case '<':
                        buff.append("&lt;");
                        break;
                    case '>':
                        buff.append("&gt;");
                        break;
                    case '\r':
                        break;
                    default:
                        buff.append((char)c);
                }//switch
            }
        }
        catch (IOException e)
        {
            //Should never happen
            Log.e(K9.LOG_TAG, "Could not read string to convert text to HTML:", e);
        }
        text = buff.toString();
        text = text.replaceAll("\\s*([-=_]{30,}+)\\s*","<hr />");
        text = text.replaceAll("(?m)^([^\r\n]{4,}[\\s\\w,:;+/])(?:\r\n|\n|\r)(?=[a-z]\\S{0,10}[\\s\\n\\r])","$1 ");
        text = text.replaceAll("(?m)(\r\n|\n|\r){4,}","\n\n");

        Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer(text.length() + 512);
        sb.append("<html><head></head><body>");
        sb.append(htmlifyMessageHeader());
        while (m.find())
        {
            int start = m.start();
            if (start == 0 || (start != 0 && text.charAt(start - 1) != '@'))
            {
                if (m.group().indexOf(':') > 0)   // With no URI-schema we may get "http:/" links with the second / missing
                {
                    m.appendReplacement(sb, "<a href=\"$0\">$0</a>");
                }
                else
                {
                    m.appendReplacement(sb, "<a href=\"http://$0\">$0</a>");
                }
            }
            else
            {
                m.appendReplacement(sb, "$0");
            }
        }

        m.appendTail(sb);
        sb.append(htmlifyMessageFooter());
        sb.append("</body></html>");
        text = sb.toString();

        return text;
    }

    private static String htmlifyMessageHeader()
    {
        if (K9.messageViewFixedWidthFont())
        {
            return "<pre style=\"white-space: pre-wrap; word-wrap:break-word; \">";
        }
        else
        {
            return "<div style=\"white-space: pre-wrap; word-wrap:break-word; \">";
        }
    }

    private static String htmlifyMessageFooter()
    {
        if (K9.messageViewFixedWidthFont())
        {
            return "</pre>";
        }
        else
        {
            return "</div>";
        }
    }

}
