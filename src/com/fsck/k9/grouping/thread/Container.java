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
    private MessageInfo<T> message;

    private Container<T> parent;

    /**
     * First child
     */
    private Container<T> child;

    /**
     * Next element in sibling list, or <code>null</code>
     */
    private Container<T> next;

    public MessageInfo<T> getMessage()
    {
        return message;
    }

    public void setMessage(MessageInfo<T> message)
    {
        this.message = message;
    }

    public Container<T> getParent()
    {
        return parent;
    }

    public void setParent(Container<T> parent)
    {
        this.parent = parent;
    }

    public Container<T> getChild()
    {
        return child;
    }

    public void setChild(Container<T> child)
    {
        this.child = child;
    }

    public Container<T> getNext()
    {
        return next;
    }

    public void setNext(Container<T> next)
    {
        this.next = next;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "[message=" + message + "]";
    }

}
