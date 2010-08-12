package com.fsck.k9.grouping;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageInfoSingletonGroup<T> implements MessageGroup<T>
{

    private final MessageInfo<T> mMessageInfo;

    private final List<MessageInfo<T>> mMessages;

    public MessageInfoSingletonGroup(final MessageInfo<T> messageInfo)
    {
        this.mMessageInfo = messageInfo;
        mMessages = Collections.singletonList(messageInfo);
    }

    @Override
    public List<MessageInfo<T>> getMessages()
    {
        return mMessages;
    }

    @Override
    public String getSubject()
    {
        return mMessageInfo.getSubject();
    }

    @Override
    public Date getDate()
    {
        return mMessageInfo.getDate();
    }

    @Override
    public int getId()
    {
        return mMessageInfo.hashCode();
    }

}
