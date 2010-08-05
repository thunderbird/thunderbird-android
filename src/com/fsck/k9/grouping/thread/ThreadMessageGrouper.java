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
                                    messages.size(), Threader.count(firstRoot, false),
                                    Threader.count(firstRoot, true), result.size(), total));
        }
        return result;
    }

    private <T> List<MessageGroup<T>> toMessageGroups(final Container<T> firstRoot)
    {
        final List<MessageGroup<T>> result = new ArrayList<MessageGroup<T>>();
        for (Container<T> root = firstRoot; root != null; root = root.getNext())
        {
            final MessageGroup<T> messageGroup = toMessageGroup(root);
            if (messageGroup != null)
            {
                result.add(messageGroup);
            }
        }
        return result;
    }

    /**
     * @param <T>
     * @param root
     *            Never <code>null</code>.
     * @return <code>null</code> if the given branch didn't contain any
     *         non-empty container (no message found).
     */
    protected <T> MessageGroup<T> toMessageGroup(final Container<T> root)
    {
        final SimpleMessageGroup<T> messageGroup = new SimpleMessageGroup<T>();
        final List<MessageInfo<T>> messages = convertToList(root); //toList(root, false);

        if (messages.isEmpty())
        {
            return null;
        }

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
     * <p>
     * This method is recursive and subject to {@link StackOverflowError} in
     * case of deep hierarchy.
     * </p>
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

    /**
     * Iterative (as in 'non-recursive') version of
     * {@link #toList(Container, boolean)}
     * 
     * @param <T>
     * @param root
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    protected <T> List<MessageInfo<T>> convertToList(final Container<T> root)
    {
        final List<MessageInfo<T>> results;

        results = Threader.walkIterative(new Threader.ContainerWalk<T, List<MessageInfo<T>>>()
        {
            private List<MessageInfo<T>> list;

            @Override
            public void init(final Container<T> root)
            {
                list = new ArrayList<MessageInfo<T>>();
            }

            @Override
            public boolean process(final Container<T> node)
            {
                final MessageInfo<T> message = node.getMessage();
                if (message != null)
                {
                    list.add(message);
                }
                return true;
            }

            @Override
            public void finish()
            {
                // no-op
            }

            @Override
            public List<MessageInfo<T>> result()
            {
                return list;
            }
        }, root, false);

        //        main: for (Container<T> current = root; current != null;)
        //        {
        //            if (current.getMessage() != null)
        //            {
        //                results.add(current.getMessage());
        //            }
        //
        //            if (current.getChild() != null)
        //            {
        //                // deeper
        //                current = current.getChild();
        //            }
        //            else if (current != root && current.getNext() != null)
        //            {
        //                // siblings
        //                current = current.getNext();
        //            }
        //            else if (current != root && current.getParent() != null)
        //            {
        //                while (current != null && current != root && current.getParent() != null)
        //                {
        //                    // back to parent
        //                    current = current.getParent();
        //                    if (current.getNext() != null)
        //                    {
        //                        // we (former parent) have siblings, cool!
        //                        break;
        //                    }
        //                }
        //                // make sure we're not back at the root
        //                if (current == root || current == null)
        //                {
        //                    break main;
        //                }
        //                // go to siblings!
        //                current = current.getNext();
        //            }
        //            else
        //            {
        //                current = null;
        //            }
        //        }

        return results;
    }

}
