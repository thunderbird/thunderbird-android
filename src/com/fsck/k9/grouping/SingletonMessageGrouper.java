package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Doesn't operate any grouping logic: a single {@link MessageGroup} will be
 * returned for the given messages.
 */
public class SingletonMessageGrouper implements MessageGrouper
{

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();

        if (messages instanceof List)
        {
            group.setMessages((List<MessageInfo<T>>) messages);
        }
        else
        {
            group.setMessages(new ArrayList<MessageInfo<T>>(messages));
        }

        // fixed values
        group.setId(0);
        group.setSubject("");

        final List<MessageGroup<T>> groups = Collections.singletonList((MessageGroup<T>) group);
        return groups;
    }

}
