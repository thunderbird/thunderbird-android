
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.MimeType;
import com.fsck.k9.mail.Multipart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jetbrains.annotations.NotNull;

import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;


/**
 * TODO this is a close approximation of Message, need to update along with
 * Message.
 */
public class MimeBodyPart extends BodyPart {
    private final MimeHeader mHeader;
    private Body mBody;

    /**
     * Creates an instance that will check the header field syntax when adding headers.
     */
    public static MimeBodyPart create(Body body) throws MessagingException {
        return new MimeBodyPart(body, null, true);
    }

    /**
     * Creates an instance that will check the header field syntax when adding headers.
     */
    public static MimeBodyPart create(Body body, String contentType) throws MessagingException {
        return new MimeBodyPart(body, contentType, true);
    }

    public MimeBodyPart() throws MessagingException {
        this(null);
    }

    public MimeBodyPart(Body body) throws MessagingException {
        this(body, null);
    }

    public MimeBodyPart(Body body, String contentType) throws MessagingException {
        this(body, contentType, false);
    }

    private MimeBodyPart(Body body, String contentType, boolean checkHeaders) throws MessagingException {
        mHeader = new MimeHeader();
        mHeader.setCheckHeaders(checkHeaders);
        if (contentType != null) {
            addHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
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

    @NotNull
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
            return contentType;
        }

        return getDefaultMimeType();
    }

    @NotNull
    private String getDefaultMimeType() {
        Multipart parent = getParent();
        if (parent != null && isSameMimeType(parent.getMimeType(), "multipart/digest")) {
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
        String mimeTypeFromHeader = MimeUtility.getHeaderParameter(getContentType(), null);
        MimeType mimeType = MimeType.parseOrNull(mimeTypeFromHeader);
        return mimeType != null ? mimeType.toString() : getDefaultMimeType();
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
