package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SimpleMessageGrouper implements MessageGrouper
{

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        final List<MessageGroup<T>> results = new ArrayList<MessageGroup<T>>(messages.size());
        for (final MessageInfo<T> messageInfo : messages)
        {
            results.add(new MessageInfoSingletonGroup<T>(messageInfo));
        }
        return results;
    }

}
