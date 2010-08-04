package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Describes a message, working unit data for grouping computations.
 * 
 * @param <T>
 *            Convenience payload data
 */
public class MessageInfo<T>
{

    private String subject;

    private Date date;

    private String id;

    private List<String> references = new ArrayList<String>();

    private T tag;

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return Message identifiers the current instance refers to. Usually not
     *         <code>null</code>.
     */
    public List<String> getReferences()
    {
        return references;
    }

    /**
     * @param references
     *            <code>null</code> should be avoided (prefer an empty
     *            {@link List}).
     */
    public void setReferences(List<String> references)
    {
        this.references = references;
    }

    /**
     * @return Arbitrary data
     */
    public T getTag()
    {
        return tag;
    }

    public void setTag(T tag)
    {
        this.tag = tag;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this))
                + "[id=" + id + "]";
    }

}
