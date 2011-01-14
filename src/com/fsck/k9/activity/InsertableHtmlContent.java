package com.fsck.k9.activity;

import java.io.Serializable;

/**
 * <p>Represents an HTML document with an insertion point for placing a reply. The quoted
 * document may have been modified to make it suitable for insertion. The modified quoted
 * document should be used in place of the original document.</p>
 *
 * <p>Changes to the user-generated inserted content should be done with {@link
 * #setUserContent(String)}.</p>
 *
 * TODO: This container should also have a text part, along with its insertion point.  Or maybe a generic InsertableContent and maintain one each for Html and Text?
 */
class InsertableHtmlContent implements Serializable
{
    private static final long serialVersionUID = 2397327034L;
    // Default to a headerInsertionPoint at the beginning of the message.
    private int headerInsertionPoint = 0;
    private int footerInsertionPoint = 0;
    // Quoted message, if any.  headerInsertionPoint refers to a position in this string.
    private StringBuilder quotedContent = new StringBuilder();
    // User content (typically their reply or comments on a forward)
    private StringBuilder userContent = new StringBuilder();

    public int getHeaderInsertionPoint()
    {
        return headerInsertionPoint;
    }

    public void setHeaderInsertionPoint(int headerInsertionPoint)
    {
        this.headerInsertionPoint = headerInsertionPoint;
    }

    public void setFooterInsertionPoint(int footerInsertionPoint)
    {
        this.footerInsertionPoint = footerInsertionPoint;
    }

    public String getQuotedContent()
    {
        return quotedContent.toString();
    }

    /**
     * Set the quoted content.  The insertion point should be set against this content.
     * @param content
     */
    public void setQuotedContent(StringBuilder content)
    {
        this.quotedContent = content;
    }

    /**
     * Insert something into the quoted content header. This is typically used for inserting
     * reply/forward headers into the quoted content rather than inserting the user-generated reply
     * content.
     * @param content
     */
    public void insertIntoQuotedHeader(final String content)
    {
        quotedContent.insert(headerInsertionPoint, content);
        // Update the location of the footer insertion point.
        footerInsertionPoint += content.length();
    }

    /**
     * Insert something into the quoted content footer. This is typically used for inserting closing
     * tags of reply/forward headers rather than inserting the user-generated reply content.
     * @param content
     */
    public void insertIntoQuotedFooter(final String content)
    {
        quotedContent.insert(footerInsertionPoint, content);
    }

    /**
     * Remove all quoted content.
     */
    public void clearQuotedContent()
    {
        quotedContent.setLength(0);
        footerInsertionPoint = 0;
        headerInsertionPoint = 0;
    }

    /**
     * Set the inserted content to the specified content. Replaces anything currently in the
     * inserted content buffer.
     * @param content
     */
    public void setUserContent(final String content)
    {
        userContent = new StringBuilder(content);
    }

    /**
     * Build the composed string with the inserted and original content.
     * @return Composed string.
     */
    @Override
    public String toString()
    {
        // Inserting and deleting was twice as fast as instantiating a new StringBuilder and
        // using substring() to build the new pieces.
        String result = quotedContent.insert(headerInsertionPoint, userContent.toString()).toString();
        quotedContent.delete(headerInsertionPoint, headerInsertionPoint + userContent.length());
        return result;
    }

    /**
     * Return debugging information for this container.
     * @return Debug string.
     */
    public String toDebugString()
    {
        return "InsertableHtmlContent{" +
               "headerInsertionPoint=" + headerInsertionPoint +
               ", footerInsertionPoint=" + footerInsertionPoint +
               ", quotedContent=" + quotedContent +
               ", userContent=" + userContent +
               ", compiledResult=" + toString() +
               '}';
    }
}
