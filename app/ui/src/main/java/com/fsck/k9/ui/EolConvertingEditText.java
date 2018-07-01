package com.fsck.k9.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * An {@link android.widget.EditText} extension with methods that convert line endings from
 * {@code \r\n} to {@code \n} and back again when setting and getting text.
 *
 */
public class EolConvertingEditText extends EditText {

    public EolConvertingEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Return the text the EolConvertingEditText is displaying.
     *
     * @return A string with any line endings converted to {@code \r\n}.
     */
    public String getCharacters() {
        return getText().toString().replace("\n", "\r\n");
    }

    /**
     * Sets the string value of the EolConvertingEditText. Any line endings
     * in the string will be converted to {@code \n}.
     *
     * @param text
     */
    public void  setCharacters(CharSequence text) {
        setText(text.toString().replace("\r\n", "\n"));
    }

}
