package com.fsck.k9.grouping;

import java.util.Date;
import java.util.List;

/**
 * An arbitrary gathering of messages.
 * 
 * @param <T>
 *            Message payload type.
 */
public interface MessageGroup<T>
{

    /**
     * @return The messages included in this group.
     */
    List<MessageInfo<T>> getMessages();

    /**
     * @return The computed subject for this group.
     */
    String getSubject();

    /**
     * @return The computed date for this group.
     */
    Date getDate();

    /**
     * @return An identifier for this group.
     */
    int getId();

}
