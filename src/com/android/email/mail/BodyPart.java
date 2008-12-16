
package com.android.email.mail;

public abstract class BodyPart implements Part {
    protected Multipart mParent;

    public Multipart getParent() {
        return mParent;
    }
}
