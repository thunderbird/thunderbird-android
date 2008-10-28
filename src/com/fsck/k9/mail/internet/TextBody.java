
package com.android.email.mail.internet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;


import com.android.email.codec.binary.Base64;
import com.android.email.mail.Body;
import com.android.email.mail.MessagingException;

public class TextBody implements Body {
    String mBody;

    public TextBody(String body) {
        this.mBody = body;
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        byte[] bytes = mBody.getBytes("UTF-8");
        out.write(Base64.encodeBase64Chunked(bytes));
    }
    
    /**
     * Get the text of the body in it's unencoded format. 
     * @return
     */
    public String getText() {
        return mBody;
    }

    /**
     * Returns an InputStream that reads this body's text in UTF-8 format.
     */
    public InputStream getInputStream() throws MessagingException {
        try {
            byte[] b = mBody.getBytes("UTF-8");
            return new ByteArrayInputStream(b);
        }
        catch (UnsupportedEncodingException usee) {
            return null;
        }
    }
}
