package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class SenderMessageGrouper implements MessageGrouper
{

    private static final Comparator<? super MessageInfo<?>> COMPARATOR = new Comparator<MessageInfo<?>>()
    {
        @Override
        public int compare(final MessageInfo<?> o1, final MessageInfo<?> o2)
        {
            int comparison;
            comparison = o1.getSender().compareToIgnoreCase(o2.getSender());
            return comparison;
        }
    };

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        final List<MessageInfo<T>> list;

        if (messages instanceof List)
        {
            // in this case, we'll be altering the argument
            list = (List<MessageInfo<T>>) messages;
        }
        else
        {
            list = new ArrayList<MessageInfo<T>>(messages);
        }

        Collections.sort(list, COMPARATOR);

        final List<MessageGroup<T>> groups = new ArrayList<MessageGroup<T>>();

        SimpleMessageGroup<T> currentGroup = null;
        List<MessageInfo<T>> currentList = null;
        MessageInfo<?> last = null;

        for (final Iterator<MessageInfo<T>> iterator = list.iterator(); iterator.hasNext();)
        {
            final MessageInfo<T> message = iterator.next();
            if (last == null || COMPARATOR.compare(message, last) != 0)
            {
                currentList = new ArrayList<MessageInfo<T>>();
                currentGroup = new SimpleMessageGroup<T>();
                currentGroup.setMessages(currentList);
                currentGroup.setDate(message.getDate());
                currentGroup.setSubject(message.getSender());
                groups.add(currentGroup);
            }
            currentList.add(message);
            last = message;
        }

        return groups;
    }

}
