
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.support.annotation.NonNull;


/**
 * TODO this is a close approximation of Message, need to update along with
 * Message.
 */
public class MimeBodyPart extends BodyPart {
    private final MimeHeader mHeader;
    private Body mBody;

    public MimeBodyPart() throws MessagingException {
        this(null);
    }

    public MimeBodyPart(Body body) throws MessagingException {
        this(body, null);
    }

    public MimeBodyPart(Body body, String mimeType) throws MessagingException {
        mHeader = new MimeHeader();
        if (mimeType != null) {
            addHeader(MimeHeader.HEADER_CONTENT_TYPE, mimeType);
        }
        MimeMessageHelper.setBody(this, body);
    }

    MimeBodyPart(MimeHeader header, Body body)  throws MessagingException {
        mHeader = header;
        MimeMessageHelper.setBody(this, body);
    }

    private String getFirstHeader(String name) {
        return mHeader.getFirstHeader(name);
    }

    @Override
    public void addHeader(String name, String value) {
        mHeader.addHeader(name, value);
    }

    @Override
    public void addRawHeader(String name, String raw) {
        mHeader.addRawHeader(name, raw);
    }

    @Override
    public void setHeader(String name, String value) {
        mHeader.setHeader(name, value);
    }

    @NonNull
    @Override
    public String[] getHeader(String name) {
        return mHeader.getHeader(name);
    }

    @Override
    public void removeHeader(String name) {
        mHeader.removeHeader(name);
    }

    @Override
    public Body getBody() {
        return mBody;
    }

    @Override
    public void setBody(Body body) {
        this.mBody = body;
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (mBody != null) {
            mBody.setEncoding(encoding);
        }
        setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
    }

    @Override
    public String getContentType() {
        String contentType = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType != null) {
            return MimeUtility.unfoldAndDecode(contentType);
        }
        Multipart parent = getParent();
        if (parent != null && "multipart/digest".equals(parent.getMimeType())) {
            return "message/rfc822";
        }
        return "text/plain";
    }

    @Override
    public String getDisposition() {
        return getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION);
    }

    @Override
    public String getContentId() {
        String contentId = getFirstHeader(MimeHeader.HEADER_CONTENT_ID);
        if (contentId == null) {
            return null;
        }

        int first = contentId.indexOf('<');
        int last = contentId.lastIndexOf('>');

        return (first != -1 && last != -1) ?
               contentId.substring(first + 1, last) :
               contentId;
    }

    @Override
    public String getMimeType() {
        return MimeUtility.getHeaderParameter(getContentType(), null);
    }

    @Override
    public boolean isMimeType(String mimeType) {
        return getMimeType().equalsIgnoreCase(mimeType);
    }

    /**
     * Write the MimeMessage out in MIME format.
     */
    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        mHeader.writeTo(out);
        writer.write("\r\n");
        writer.flush();
        if (mBody != null) {
            mBody.writeTo(out);
        }
    }

    @Override
    public void writeHeaderTo(OutputStream out) throws IOException, MessagingException {
        mHeader.writeTo(out);
    }

}
