package com.fsck.k9.grouping;

import java.util.Collection;
import java.util.List;

/**
 * Grouping computations.
 */
public interface MessageGrouper
{

    /**
     * Given a message list, compute them into groups. Grouping logic is
     * implementation-dependent.
     *
     * @param <T>
     * @param messages
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    <T> List<MessageGroup<T>> group(Collection<MessageInfo<T>> messages);

}
