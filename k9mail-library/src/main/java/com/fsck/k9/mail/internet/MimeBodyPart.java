
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.CompositeBody;
import com.fsck.k9.mail.MessagingException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

import org.apache.james.mime4j.util.MimeUtil;

/**
 * TODO this is a close approximation of Message, need to update along with
 * Message.
 */
public class MimeBodyPart extends BodyPart {
    private final MimeHeader mHeader = new MimeHeader();
    private Body mBody;

    public MimeBodyPart() throws MessagingException {
        this(null);
    }

    public MimeBodyPart(Body body) throws MessagingException {
        this(body, null);
    }

    public MimeBodyPart(Body body, String mimeType) throws MessagingException {
        if (mimeType != null) {
            addHeader(MimeHeader.HEADER_CONTENT_TYPE, mimeType);
        }
        MimeMessageHelper.setBody(this, body);
    }

    private String getFirstHeader(String name) {
        return mHeader.getFirstHeader(name);
    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
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

    @Override
    public String[] getHeader(String name) throws MessagingException {
        return mHeader.getHeader(name);
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
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
        return (contentType == null) ? "text/plain" : contentType;
    }

    @Override
    public String getDisposition() throws MessagingException {
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
    public boolean isMimeType(String mimeType) throws MessagingException {
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

    @Override
    public void setUsing7bitTransport() throws MessagingException {
        String type = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        /*
         * We don't trust that a multipart/* will properly have an 8bit encoding
         * header if any of its subparts are 8bit, so we automatically recurse
         * (as long as its not multipart/signed).
         */
        if (mBody instanceof CompositeBody
                && !"multipart/signed".equalsIgnoreCase(type)) {
            setEncoding(MimeUtil.ENC_7BIT);
            // recurse
            ((CompositeBody) mBody).setUsing7bitTransport();
        } else if (!MimeUtil.ENC_8BIT
                .equalsIgnoreCase(getFirstHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING))) {
            return;
        } else if (type != null
                && (type.equalsIgnoreCase("multipart/signed") || type
                        .toLowerCase(Locale.US).startsWith("message/"))) {
            /*
             * This shouldn't happen. In any case, it would be wrong to convert
             * them to some other encoding for 7bit transport.
             *
             * RFC 1847 says multipart/signed must be 7bit. It also says their
             * bodies must be treated as opaque, so we must not change the
             * encoding.
             *
             * We've dealt with (CompositeBody) type message/rfc822 above. Here
             * we must deal with all other message/* types. RFC 2045 says
             * message/* can only be 7bit or 8bit. RFC 2046 says unknown
             * message/* types must be treated as application/octet-stream,
             * which means we can't recurse into them. It also says that
             * existing subtypes message/partial and message/external must only
             * be 7bit, and that future subtypes "should be" 7bit.
             */
            throw new MessagingException(
                    "Unable to convert 8bit body part to 7bit");
        } else {
            setEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
        }
    }
}
