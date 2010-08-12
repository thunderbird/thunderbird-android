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
    private MessageInfo<T> mRessage;

    private Container<T> mParent;

    /**
     * First child
     */
    private Container<T> mChild;

    /**
     * Next element in sibling list, or <code>null</code>
     */
    private Container<T> mNext;

    public MessageInfo<T> getMessage()
    {
        return mRessage;
    }

    public void setMessage(MessageInfo<T> message)
    {
        this.mRessage = message;
    }

    public Container<T> getParent()
    {
        return mParent;
    }

    public void setParent(Container<T> parent)
    {
        this.mParent = parent;
    }

    public Container<T> getChild()
    {
        return mChild;
    }

    public void setChild(Container<T> child)
    {
        this.mChild = child;
    }

    public Container<T> getNext()
    {
        return mNext;
    }

    public void setNext(Container<T> next)
    {
        this.mNext = next;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "[message=" + mRessage + "]";
    }

}
