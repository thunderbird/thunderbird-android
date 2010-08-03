package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Working data for grouping computations.
 *
 * @param <T>
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

    public List<String> getReferences()
    {
        return references;
    }

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
