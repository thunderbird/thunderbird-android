
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.fsck.k9.mail.filter.CountingOutputStream;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

public class TextBody implements Body, SizeAware {

    /**
     * Immutable empty byte array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final String mBody;
    private String mEncoding;
    private String mCharset = "UTF-8";
    // Length of the message composed (as opposed to quoted). I don't like the name of this variable and am open to
    // suggestions as to what it should otherwise be. -achen 20101207
    private Integer mComposedMessageLength;
    // Offset from position 0 where the composed message begins.
    private Integer mComposedMessageOffset;

    public TextBody(String body) {
        this.mBody = body;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (mBody != null) {
            byte[] bytes = mBody.getBytes(mCharset);
            if (MimeUtil.ENC_8BIT.equalsIgnoreCase(mEncoding)) {
                out.write(bytes);
            } else {
                QuotedPrintableOutputStream qp = new QuotedPrintableOutputStream(out, false);
                qp.write(bytes);
                qp.flush();
                qp.close();
            }
        }
    }

    /**
     * Get the text of the body in it's unencoded format.
     * @return
     */
    public String getText() {
        return mBody;
    }

    /**
     * Returns an InputStream that reads this body's text.
     */
    @Override
    public InputStream getInputStream() throws MessagingException {
        try {
            byte[] b;
            if (mBody != null) {
                b = mBody.getBytes(mCharset);
            } else {
                b = EMPTY_BYTE_ARRAY;
            }
            return new ByteArrayInputStream(b);
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    @Override
    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    public void setCharset(String charset) {
        mCharset = charset;
    }

    public Integer getComposedMessageLength() {
        return mComposedMessageLength;
    }

    public void setComposedMessageLength(Integer composedMessageLength) {
        this.mComposedMessageLength = composedMessageLength;
    }

    public Integer getComposedMessageOffset() {
        return mComposedMessageOffset;
    }

    public void setComposedMessageOffset(Integer composedMessageOffset) {
        this.mComposedMessageOffset = composedMessageOffset;
    }

    @Override
    public long getSize() {
        try {
            byte[] bytes = mBody.getBytes(mCharset);

            if (MimeUtil.ENC_8BIT.equalsIgnoreCase(mEncoding)) {
                return bytes.length;
            } else {
                return getLengthWhenQuotedPrintableEncoded(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get body size", e);
        }
    }

    private long getLengthWhenQuotedPrintableEncoded(byte[] bytes) throws IOException {
        CountingOutputStream countingOutputStream = new CountingOutputStream();
        OutputStream quotedPrintableOutputStream = new QuotedPrintableOutputStream(countingOutputStream, false);
        try {
            quotedPrintableOutputStream.write(bytes);
        } finally {
            try {
                quotedPrintableOutputStream.close();
            } catch (IOException e) { /* ignore */ }
        }

        return countingOutputStream.getCount();
    }
}
