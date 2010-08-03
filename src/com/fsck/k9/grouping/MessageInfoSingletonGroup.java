package com.fsck.k9.grouping;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageInfoSingletonGroup<T> implements MessageGroup<T>
{

    private final MessageInfo<T> messageInfo;

    private final List<MessageInfo<T>> messages;

    public MessageInfoSingletonGroup(final MessageInfo<T> messageInfo)
    {
        this.messageInfo = messageInfo;
        messages = Collections.singletonList(messageInfo);
    }

    @Override
    public List<MessageInfo<T>> getMessages()
    {
        return messages;
    }

    @Override
    public String getSubject()
    {
        return messageInfo.getSubject();
    }

    @Override
    public Date getDate()
    {
        return messageInfo.getDate();
    }

}
