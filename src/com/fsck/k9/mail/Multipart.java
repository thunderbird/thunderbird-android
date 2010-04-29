
package com.fsck.k9.mail;

import java.util.ArrayList;

import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.TextBody;

public abstract class Multipart implements Body
{
    protected Part mParent;

    protected ArrayList<BodyPart> mParts = new ArrayList<BodyPart>();

    protected String mContentType;

    public void addBodyPart(BodyPart part) throws MessagingException
    {
        mParts.add(part);
    }

    public void addBodyPart(BodyPart part, int index) throws MessagingException
    {
        mParts.add(index, part);
    }

    public BodyPart getBodyPart(int index) throws MessagingException
    {
        return mParts.get(index);
    }

    public String getContentType() throws MessagingException
    {
        return mContentType;
    }

    public int getCount() throws MessagingException
    {
        return mParts.size();
    }

    public boolean removeBodyPart(BodyPart part) throws MessagingException
    {
        return mParts.remove(part);
    }

    public void removeBodyPart(int index) throws MessagingException
    {
        mParts.remove(index);
    }

    public Part getParent() throws MessagingException
    {
        return mParent;
    }

    public void setParent(Part parent) throws MessagingException
    {
        this.mParent = parent;
    }

    public void setEncoding(String encoding)
    {
        for (BodyPart part : mParts)
        {
            try
            {
                Body body = part.getBody();
                if (body instanceof TextBody)
                {
                    part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
                    ((TextBody)body).setEncoding(encoding);
                }
            }
            catch (MessagingException e)
            {
                // Ignore
            }
        }

    }
}
