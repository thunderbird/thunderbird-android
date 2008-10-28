
package com.android.email.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.android.email.mail.BodyPart;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Multipart;

public class MimeMultipart extends Multipart {
    protected String mPreamble;

    protected String mContentType;

    protected String mBoundary;

    protected String mSubType;

    public MimeMultipart() throws MessagingException {
        mBoundary = generateBoundary();
        setSubType("mixed");
    }

    public MimeMultipart(String contentType) throws MessagingException {
        this.mContentType = contentType;
        try {
            mSubType = MimeUtility.getHeaderParameter(contentType, null).split("/")[1];
            mBoundary = MimeUtility.getHeaderParameter(contentType, "boundary");
            if (mBoundary == null) {
                throw new MessagingException("MultiPart does not contain boundary: " + contentType);
            }
        } catch (Exception e) {
            throw new MessagingException(
                    "Invalid MultiPart Content-Type; must contain subtype and boundary. ("
                            + contentType + ")", e);
        }
    }

    public String generateBoundary() {
        StringBuffer sb = new StringBuffer();
        sb.append("----");
        for (int i = 0; i < 30; i++) {
            sb.append(Integer.toString((int)(Math.random() * 35), 36));
        }
        return sb.toString().toUpperCase();
    }

    public String getPreamble() throws MessagingException {
        return mPreamble;
    }

    public void setPreamble(String preamble) throws MessagingException {
        this.mPreamble = preamble;
    }

    public String getContentType() throws MessagingException {
        return mContentType;
    }

    public void setSubType(String subType) throws MessagingException {
        this.mSubType = subType;
        mContentType = String.format("multipart/%s; boundary=\"%s\"", subType, mBoundary);
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);

        if (mPreamble != null) {
            writer.write(mPreamble + "\r\n");
        }

        for (int i = 0, count = mParts.size(); i < count; i++) {
            BodyPart bodyPart = (BodyPart)mParts.get(i);
            writer.write("--" + mBoundary + "\r\n");
            writer.flush();
            bodyPart.writeTo(out);
            writer.write("\r\n");
        }

        writer.write("--" + mBoundary + "--\r\n");
        writer.flush();
    }

    public InputStream getInputStream() throws MessagingException {
        return null;
    }
}
