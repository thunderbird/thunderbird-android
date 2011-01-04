package com.fsck.k9.helper;

import android.text.Annotation;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import org.xml.sax.XMLReader;

/**
 * Custom tag handler to use when converting HTML messages to text. It currently handles text
 * representations of HTML tags that Android's built-in parser doesn't understand and hides code
 * contained in STYLE and SCRIPT blocks.
 */
public class HtmlToTextTagHandler implements Html.TagHandler
{
    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader)
    {
        if (tag.equalsIgnoreCase("hr") && opening)
        {
            // In the case of an <hr>, replace it with a bunch of underscores.  This is roughly the behaviour of
            // Outlook in Rich Text mode.
            output.append("_____________________________________________\n");
        }
        else if (tag.equalsIgnoreCase("style") || tag.equalsIgnoreCase("script"))
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
