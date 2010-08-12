package com.fsck.k9.grouping.thread;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.grouping.MessageGroup;
import com.fsck.k9.grouping.MessageGrouper;
import com.fsck.k9.grouping.MessageInfo;
import com.fsck.k9.grouping.SimpleMessageGroup;
import com.fsck.k9.helper.Utility;

public class ThreadMessageGrouper implements MessageGrouper
{

    private static final class DateComparator<T> implements Comparator<MessageInfo<T>>
    {
        @Override
        public int compare(final MessageInfo<T> object1, final MessageInfo<T> object2)
        {
            return object1.getDate().compareTo(object2.getDate());
        }
    }

    private Threader mThreader = new Threader();

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        if (messages.isEmpty())
        {
            return Collections.emptyList();
        }

        final Container<T> fakeRoot = mThreader.thread(messages, true);

        final List<MessageGroup<T>> result = toMessageGroups(fakeRoot, messages);

        if (K9.DEBUG)
        {
            int total = 0;
            for (final MessageGroup<T> messageGroup : result)
            {
                total += messageGroup.getMessages().size();
            }
            Log.v(K9.LOG_TAG,
                    MessageFormat
                            .format("Grouping result: input={0} w/o-empty={1} w/-empty={2} groups={3} resulting={4}",
                                    messages.size(), Threader.count(fakeRoot, false),
                                    Threader.count(fakeRoot, true), result.size(), total));
        }
        return result;
    }

    private <T> List<MessageGroup<T>> toMessageGroups(final Container<T> fakeRoot,
            Collection<MessageInfo<T>> originalList)
    {
        final List<MessageGroup<T>> result = new ArrayList<MessageGroup<T>>();
        for (Container<T> root = fakeRoot.getChild(); root != null; root = root.getNext())
        {
            final MessageGroup<T> messageGroup = toMessageGroup(root, null);
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
     * @param originalList
     *            TODO
     * @return <code>null</code> if the given branch didn't contain any
     *         non-empty container (no message found).
     */
    protected <T> MessageGroup<T> toMessageGroup(final Container<T> root,
            Collection<MessageInfo<T>> originalList)
    {
        final SimpleMessageGroup<T> messageGroup = new SimpleMessageGroup<T>();
        final List<MessageInfo<T>> messages = convertToList(root); //toList(root, false);

        if (messages.isEmpty())
        {
            return null;
        }

        // sorting, but we need to keep the same first one as found by the
        // threading algorithm
        final MessageInfo<T> first = messages.remove(0);
        Collections.sort(messages, new DateComparator<T>());
        messages.add(0, first);

        messageGroup.setMessages(messages);

        final String subject = Utility.stripSubject(first.getSubject());
        messageGroup.setSubject(subject);

        final MessageInfo<T> last = messages.get(messages.size() - 1);
        messageGroup.setDate(last.getDate());

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
            public WalkAction processRoot(final Container<T> root)
            {
                list = new ArrayList<MessageInfo<T>>();

                add(root);

                return WalkAction.CONTINUE;
            }

            @Override
            public WalkAction processNode(final Container<T> node)
            {
                add(node);
                return WalkAction.CONTINUE;
            }

            private void add(final Container<T> node)
            {
                final MessageInfo<T> message = node.getMessage();
                if (message != null)
                {
                    list.add(message);
                }
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
        }, root);

        return results;
    }

}
