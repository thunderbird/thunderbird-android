package com.fsck.k9.grouping.thread;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.grouping.MessageGroup;
import com.fsck.k9.grouping.MessageGrouper;
import com.fsck.k9.grouping.MessageInfo;
import com.fsck.k9.grouping.SimpleMessageGroup;

public class ThreadMessageGrouper implements MessageGrouper
{

    private Threader threader = new Threader();

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        if (messages.isEmpty())
        {
            return Collections.emptyList();
        }

        final Container<T> firstRoot = threader.thread(messages);

        final List<MessageGroup<T>> result = toMessageGroups(firstRoot);

        if (K9.DEBUG && Log.isLoggable(K9.LOG_TAG, Log.VERBOSE))
        {
            int total = 0;
            for (final MessageGroup<T> messageGroup : result)
            {
                total += messageGroup.getMessages().size();
            }
            Log.v(K9.LOG_TAG,
                    MessageFormat
                            .format("Grouping result: input={0} w/o-empty={1} w/-empty={2} groups={3} resulting={4}",
                                    messages.size(), Threader.count(firstRoot, 0, false),
                                    Threader.count(firstRoot, 0, true), result.size(), total));
        }
        return result;
    }

    private <T> List<MessageGroup<T>> toMessageGroups(final Container<T> firstRoot)
    {
        final List<MessageGroup<T>> result = new ArrayList<MessageGroup<T>>();
        for (Container<T> root = firstRoot; root != null; root = root.getNext())
        {
            final MessageGroup<T> messageGroup = toMessageGroup(root);
            result.add(messageGroup);
        }
        return result;
    }

    /**
     * @param <T>
     * @param root
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    protected <T> MessageGroup<T> toMessageGroup(final Container<T> root)
    {
        final SimpleMessageGroup<T> messageGroup = new SimpleMessageGroup<T>();
        final List<MessageInfo<T>> messages = toList(root, false);

        messageGroup.setMessages(messages);

        // we assume there is at least 1 message in the given hierarchy
        final String subject = messages.get(0).getSubject();
        messageGroup.setSubject(subject);

        // since we grouped by subject, using it as an identifier
        messageGroup.setId(subject.hashCode());

        return messageGroup;
    }

    /**
     * Convert the linked list into a proper {@link List}. Children get
     * flattened. Empty messages are ignored.
     * 
     * @param <T>
     * @param current
     *            Never <code>null</code>.
     * @param followSiblings
     * @return Never <code>null</code>.
     */
    protected <T> List<MessageInfo<T>> toList(final Container<T> current,
            final boolean followSiblings)
    {
        final List<MessageInfo<T>> results = new ArrayList<MessageInfo<T>>();

        if (current.getMessage() != null)
        {
            // add current node
            results.add(current.getMessage());
        }

        if (current.getChild() != null)
        {
            results.addAll(toList(current.getChild(), true));
        }
        if (followSiblings && current.getNext() != null)
        {
            results.addAll(toList(current.getNext(), true));
        }
        return results;
    }
}
