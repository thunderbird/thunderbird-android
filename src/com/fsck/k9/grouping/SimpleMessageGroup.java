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

    private List<MessageInfo<T>> messages;

    private String subject;

    private Date date;

    @Override
    public List<MessageInfo<T>> getMessages()
    {
        return messages;
    }

    public void setMessages(List<MessageInfo<T>> list)
    {
        this.messages = list;
    }

    @Override
    public String getSubject()
    {
        return subject;
    }

    @Override
    public Date getDate()
    {
        return date;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

}
