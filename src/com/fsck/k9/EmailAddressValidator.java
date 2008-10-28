
package com.fsck.k9;

import com.fsck.k9.mail.Address;

import android.util.Config;
import android.util.Log;
import android.widget.AutoCompleteTextView.Validator;

public class EmailAddressValidator implements Validator {
    public CharSequence fixText(CharSequence invalidText) {
        return "";
    }

    public boolean isValid(CharSequence text) {
        return Address.parse(text.toString()).length > 0;
    }
}
