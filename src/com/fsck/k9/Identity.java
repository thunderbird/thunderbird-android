package com.fsck.k9;

import java.io.Serializable;

public class Identity implements Serializable
{
    private String mDescription;
    private String mName;
    private String mEmail;
    private String mSignature;
    private boolean mSignatureUse;
    private String replyTo;

    public synchronized String getName()
    {
        return mName;
    }

    public synchronized void setName(String name)
    {
        mName = name;
    }

    public synchronized String getEmail()
    {
        return mEmail;
    }

    public synchronized void setEmail(String email)
    {
        mEmail = email;
    }

    public synchronized boolean getSignatureUse()
    {
        return mSignatureUse;
    }

    public synchronized void setSignatureUse(boolean signatureUse)
    {
        mSignatureUse = signatureUse;
    }

    public synchronized String getSignature()
    {
        return mSignature;
    }

    public synchronized void setSignature(String signature)
    {
        mSignature = signature;
    }

    public synchronized String getDescription()
    {
        return mDescription;
    }

    public synchronized void setDescription(String description)
    {
        mDescription = description;
    }

    public synchronized String getReplyTo()
    {
        return replyTo;
    }

    public synchronized void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    @Override
    public synchronized String toString()
    {
        return "Account.Identity(description=" + mDescription + ", name=" + mName + ", email=" + mEmail + ", replyTo=" + replyTo + ", signature=" + mSignature;
    }
}
