
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;

import java.io.*;

import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;

public class TextBody implements Body
{

    /**
     * Immutable empty byte array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private String mBody;
    private String mEncoding;
    private String mCharset = "UTF-8";

    public TextBody(String body)
    {
        this.mBody = body;
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException
    {
        if (mBody != null)
        {
            byte[] bytes = mBody.getBytes(mCharset);
            if ("8bit".equals(mEncoding))
            {
                out.write(bytes);
            }
            else
            {
                QuotedPrintableOutputStream qp = new QuotedPrintableOutputStream(out, false);
                qp.write(bytes);
                qp.flush();
            }
        }
    }

    /**
     * Get the text of the body in it's unencoded format.
     * @return
     */
    public String getText()
    {
        return mBody;
    }

    /**
     * Returns an InputStream that reads this body's text.
     */
    public InputStream getInputStream() throws MessagingException
    {
        try
        {
            byte[] b;
            if (mBody!=null)
            {
                b = mBody.getBytes(mCharset);
            }
            else
            {
                b = EMPTY_BYTE_ARRAY;
            }
            return new ByteArrayInputStream(b);
        }
        catch (UnsupportedEncodingException usee)
        {
            return null;
        }
    }

    public void setEncoding(String encoding)
    {
        mEncoding = encoding;
    }

    public void setCharset(String charset)
    {
        mCharset = charset;
    }
}
