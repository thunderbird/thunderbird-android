
package com.fsck.k9;

import java.util.regex.Pattern;

import android.widget.AutoCompleteTextView.Validator;

public class EmailAddressValidator implements Validator
{
    // Source: http://www.regular-expressions.info/email.html
    private static Pattern p = Pattern.compile(
            "[a-z0-9!#$%&'*+/=?^_`{|}~-]+" +
            "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
            "@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+" +
            "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");

    public CharSequence fixText(CharSequence invalidText)
    {
        return "";
    }

    public boolean isValid(CharSequence text)
    {
        return p.matcher(text).matches();
    }
}
