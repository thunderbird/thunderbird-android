
package com.fsck.k9.mail.internet;

import com.fsck.k9.codec.binary.Base64;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;

import java.io.*;

public class TextBody implements Body
{
    private String mBody;
    private String mEncoding;

    public TextBody(String body)
    {
        this.mBody = body;
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException
    {
        if (mBody != null)
        {
            byte[] bytes = mBody.getBytes("UTF-8");
            if ("8bit".equals(mEncoding))
            {
                out.write(bytes);
            }
            else
            {
                out.write(Base64.encodeBase64Chunked(bytes));
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
     * Returns an InputStream that reads this body's text in UTF-8 format.
     */
    public InputStream getInputStream() throws MessagingException
    {
        try
        {
            byte[] b;
            if (mBody!=null)
            {
                b = mBody.getBytes("UTF-8");
            }
            else
            {
                b = new byte[0];
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
}
