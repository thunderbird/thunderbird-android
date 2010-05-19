
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

public class Utility
{
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
        for (int i = 0, count = a.length; i < count; i++)
        {
            if (a[i].equals(o))
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

}
