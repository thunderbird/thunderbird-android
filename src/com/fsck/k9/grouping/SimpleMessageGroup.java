package com.fsck.k9.grouping;

import java.util.Date;
import java.util.List;

/**
 * Simple {@link MessageGroup} implementation with static embedded data.
 * 
 * @param <T>
 */
public class SimpleMessageGroup<T> implements MessageGroup<T>
{

    private List<MessageInfo<T>> mMessages;

    private String mSubject;

    private Date mDate;

    private int mId;

    @Override
    public List<MessageInfo<T>> getMessages()
    {
        return mMessages;
    }

    public void setMessages(List<MessageInfo<T>> list)
    {
        this.mMessages = list;
    }

    @Override
    public String getSubject()
    {
        return mSubject;
    }

    @Override
    public Date getDate()
    {
        return mDate;
    }

    @Override
    public int getId()
    {
        return mId;
    }

    public void setSubject(String subject)
    {
        this.mSubject = subject;
    }

    public void setDate(Date date)
    {
        this.mDate = date;
    }

    public void setId(int id)
    {
        this.mId = id;
    }

}
