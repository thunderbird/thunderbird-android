package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Describes a message, working unit data for grouping computations.
 * 
 * @param <T>
 *            Convenience payload data
 */
public class MessageInfo<T>
{

    private String mSubject;

    private Date mDate;

    private String mId;

    private List<String> mReferences = new ArrayList<String>();

    private String mSender;

    private T mTag;

    public String getSubject()
    {
        return mSubject;
    }

    public void setSubject(String subject)
    {
        this.mSubject = subject;
    }

    public Date getDate()
    {
        return mDate;
    }

    public void setDate(Date date)
    {
        this.mDate = date;
    }

    public String getId()
    {
        return mId;
    }

    public void setId(String id)
    {
        this.mId = id;
    }

    /**
     * @return Message identifiers the current instance refers to. Usually not
     *         <code>null</code>.
     */
    public List<String> getReferences()
    {
        return mReferences;
    }

    /**
     * @param references
     *            <code>null</code> should be avoided (prefer an empty
     *            {@link List}).
     */
    public void setReferences(List<String> references)
    {
        this.mReferences = references;
    }

    public String getSender()
    {
        return mSender;
    }

    public void setSender(String sender)
    {
        this.mSender = sender;
    }

    /**
     * @return Arbitrary data
     */
    public T getTag()
    {
        return mTag;
    }

    public void setTag(T tag)
    {
        this.mTag = tag;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this))
                + "[id=" + mId + "]";
    }

}
