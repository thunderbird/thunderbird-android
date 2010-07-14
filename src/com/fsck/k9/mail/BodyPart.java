
package com.fsck.k9.mail;

public abstract class BodyPart implements Part
{
    protected Multipart mParent;

    public Multipart getParent()
    {
        return mParent;
    }
}
