
package com.fsck.k9.helper;

import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;
import com.fsck.k9.mail.filter.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility
{

    // \u00A0 (non-breaking space) happens to be used by French MUA

    // Note: no longer using the ^ beginning character combined with (...)+
    // repetition matching as we might want to strip ML tags. Ex:
    // Re: [foo] Re: RE : [foo] blah blah blah
    private static final Pattern RESPONSE_PATTERN = Pattern.compile(
            "(Re|Fw|Fwd|Aw)(\\[\\d+\\])?[\\u00A0 ]?: *", Pattern.CASE_INSENSITIVE);

    /**
     * Mailing-list tag pattern to match strings like "[foobar] "
     */
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[[-_a-z0-9]+\\] ",
            Pattern.CASE_INSENSITIVE);

    public final static String readInputStream(InputStream in, String encoding) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        StringBuffer sb = new StringBuffer();
        int count;
        char[] buf = new char[512];
        while ((count = reader.read(buf)) != -1)
        {
            sb.append(buf, 0, count);
        }
        return sb.toString();
    }

    public final static boolean arrayContains(Object[] a, Object o)
    {
        for (Object element : a)
        {
            if (element.equals(o))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Combines the given array of Objects into a single string using the
     * seperator character and each Object's toString() method. between each
     * part.
     *
     * @param parts
     * @param seperator
     * @return
     */
    public static String combine(Object[] parts, char seperator)
    {
        if (parts == null)
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++)
        {
            sb.append(parts[i].toString());
            if (i < parts.length - 1)
            {
                sb.append(seperator);
            }
        }
        return sb.toString();
    }

    public static String base64Decode(String encoded)
    {
        if (encoded == null)
        {
            return null;
        }
        byte[] decoded = new Base64().decode(encoded.getBytes());
        return new String(decoded);
    }

    public static String base64Encode(String s)
    {
        if (s == null)
        {
            return s;
        }
        byte[] encoded = new Base64().encode(s.getBytes());
        return new String(encoded);
    }

    public static boolean requiredFieldValid(TextView view)
    {
        return view.getText() != null && view.getText().length() > 0;
    }


    public static boolean requiredFieldValid(Editable s)
    {
        return s != null && s.length() > 0;
    }

    public static boolean domainFieldValid(EditText view)
    {
        if (view.getText() != null)
        {
            String s = view.getText().toString();
            if (s.matches("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$"))
            {
                return true;
            }
            if (s.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))
            {
                return true;
            }
            if ((s.equalsIgnoreCase("localhost"))||(s.equalsIgnoreCase("localhost.localdomain")))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensures that the given string starts and ends with the double quote character. The string is not modified in any way except to add the
     * double quote character to start and end if it's not already there.
     * sample -> "sample"
     * "sample" -> "sample"
     * ""sample"" -> "sample"
     * "sample"" -> "sample"
     * sa"mp"le -> "sa"mp"le"
     * "sa"mp"le" -> "sa"mp"le"
     * (empty string) -> ""
     * " -> ""
     * @param s
     * @return
     */
    public static String quoteString(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (!s.matches("^\".*\"$"))
        {
            return "\"" + s + "\"";
        }
        else
        {
            return s;
        }
    }

    /**
     * A fast version of  URLDecoder.decode() that works only with UTF-8 and does only two
     * allocations. This version is around 3x as fast as the standard one and I'm using it
     * hundreds of times in places that slow down the UI, so it helps.
     */
    public static String fastUrlDecode(String s)
    {
        try
        {
            byte[] bytes = s.getBytes("UTF-8");
            byte ch;
            int length = 0;
            for (int i = 0, count = bytes.length; i < count; i++)
            {
                ch = bytes[i];
                if (ch == '%')
                {
                    int h = (bytes[i + 1] - '0');
                    int l = (bytes[i + 2] - '0');
                    if (h > 9)
                    {
                        h -= 7;
                    }
                    if (l > 9)
                    {
                        l -= 7;
                    }
                    bytes[length] = (byte)((h << 4) | l);
                    i += 2;
                }
                else if (ch == '+')
                {
                    bytes[length] = ' ';
                }
                else
                {
                    bytes[length] = bytes[i];
                }
                length++;
            }
            return new String(bytes, 0, length, "UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            return null;
        }
    }

    /**
     * Returns true if the specified date is within 18 hours of "now". Returns false otherwise.
     * @param date
     * @return
     */
    public static boolean isDateToday(Date date)
    {
        Date now = new Date();
        if (now.getTime() - 64800000 > date.getTime() || now.getTime() + 64800000 < date.getTime())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /*
     * TODO disabled this method globally. It is used in all the settings screens but I just
     * noticed that an unrelated icon was dimmed. Android must share drawables internally.
     */
    public static void setCompoundDrawablesAlpha(TextView view, int alpha)
    {
//        Drawable[] drawables = view.getCompoundDrawables();
//        for (Drawable drawable : drawables) {
//            if (drawable != null) {
//                drawable.setAlpha(alpha);
//            }
//        }
    }

    /**
     * Extract the 'original' subject value, by ignoring leading
     * response/forward marker and '[XX]' formatted tags (as many mailing-list
     * softwares do).
     * 
     * <p>
     * Result is also trimmed.
     * </p>
     * 
     * @param subject
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    public static String stripSubject(final String subject)
    {
        int lastPrefix = 0;

        final Matcher tagMatcher = TAG_PATTERN.matcher(subject);
        String tag = null;
        // whether tag stripping logic should be active
        boolean tagPresent = false;
        // whether the last action stripped a tag
        boolean tagStripped = false;
        if (tagMatcher.find(0))
        {
            tagPresent = true;
            if (tagMatcher.start() == 0)
            {
                // found at beginning of subject, considering it an actual tag
                tag = tagMatcher.group();

                // now need to find response marker after that tag
                lastPrefix = tagMatcher.end();
                tagStripped = true;
            }
        }

        final Matcher matcher = RESPONSE_PATTERN.matcher(subject);

        // while:
        // - lastPrefix is within the bounds
        // - response marker found at lastPrefix position
        // (to make sure we don't catch response markers that are part of
        // the actual subject)

        while (lastPrefix < subject.length() - 1
                && matcher.find(lastPrefix)
                && matcher.start() == lastPrefix
                && (!tagPresent || tag == null || subject.regionMatches(matcher.end(), tag, 0,
                        tag.length())))
        {
            lastPrefix = matcher.end();

            if (tagPresent)
            {
                tagStripped = false;
                if (tag == null)
                {
                    // attempt to find tag
                    if (tagMatcher.start() == lastPrefix)
                    {
                        tag = tagMatcher.group();
                        lastPrefix += tag.length();
                        tagStripped = true;
                    }
                }
                else if (lastPrefix < subject.length() - 1 && subject.startsWith(tag, lastPrefix))
                {
                    // Re: [foo] Re: [foo] blah blah blah
                    //               ^     ^
                    //               ^     ^
                    //               ^    new position
                    //               ^
                    //              initial position
                    lastPrefix += tag.length();
                    tagStripped = true;
                }
            }
        }
        if (tagStripped)
        {
            // restore the last tag
            lastPrefix -= tag.length();
        }
        if (lastPrefix > -1 && lastPrefix < subject.length() - 1)
        {
            return subject.substring(lastPrefix).trim();
        }
        else
        {
            return subject.trim();
        }
    }

}
