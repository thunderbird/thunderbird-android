package com.fsck.k9.grouping.thread;

import com.fsck.k9.grouping.MessageInfo;

/**
 * 
 * 
 * @param <T>
 */
public class Container<T>
{
    /**
     * Can be <code>null</code>
     */
    public MessageInfo<T> message;

    public Container<T> parent;

    /**
     * First child
     */
    public Container<T> child;

    /**
     * Next element in sibling list, or <code>null</code>
     */
    public Container<T> next;

    @Override
    public String toString()
    {
        return getClass().getName() + "[message=" + message + "]";
    }

}
