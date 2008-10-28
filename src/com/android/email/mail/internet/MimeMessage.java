
package com.android.email.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import org.apache.james.mime4j.BodyDescriptor;
import org.apache.james.mime4j.ContentHandler;
import org.apache.james.mime4j.EOLConvertingInputStream;
import org.apache.james.mime4j.MimeStreamParser;
import org.apache.james.mime4j.field.DateTimeField;
import org.apache.james.mime4j.field.Field;

import com.android.email.mail.Address;
import com.android.email.mail.Body;
import com.android.email.mail.BodyPart;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;

/**
 * An implementation of Message that stores all of it's metadata in RFC 822 and
 * RFC 2045 style headers.
 */
public class MimeMessage extends Message {
    protected MimeHeader mHeader = new MimeHeader();
    protected Address[] mFrom;
    protected Address[] mTo;
    protected Address[] mCc;
    protected Address[] mBcc;
    protected Address[] mReplyTo;
    protected Date mSentDate;
    protected SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    protected Body mBody;
    protected int mSize;

    public MimeMessage() {
        /*
         * Every new messages gets a Message-ID
         */
        try {
            setHeader("Message-ID", generateMessageId());
        }
        catch (MessagingException me) {
            throw new RuntimeException("Unable to create MimeMessage", me);
        }
    }

    private String generateMessageId() {
        StringBuffer sb = new StringBuffer();
        sb.append("<");
        for (int i = 0; i < 24; i++) {
            sb.append(Integer.toString((int)(Math.random() * 35), 36));
        }
        sb.append(".");
        sb.append(Long.toString(System.currentTimeMillis()));
        sb.append("@email.android.com>");
        return sb.toString();
    }

    /**
     * Parse the given InputStream using Apache Mime4J to build a MimeMessage.
     *
     * @param in
     * @throws IOException
     * @throws MessagingException
     */
    public MimeMessage(InputStream in) throws IOException, MessagingException {
        parse(in);
    }

    protected void parse(InputStream in) throws IOException, MessagingException {
        mHeader.clear();
        mBody = null;
        mBcc = null;
        mTo = null;
        mFrom = null;
        mSentDate = null;

        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(new MimeMessageBuilder());
        parser.parse(new EOLConvertingInputStream(in));
    }

    public Date getReceivedDate() throws MessagingException {
        return null;
    }

    public Date getSentDate() throws MessagingException {
        if (mSentDate == null) {
            try {
                DateTimeField field = (DateTimeField)Field.parse("Date: "
                        + MimeUtility.unfoldAndDecode(getFirstHeader("Date")));
                mSentDate = field.getDate();
            } catch (Exception e) {

            }
        }
        return mSentDate;
    }

    public void setSentDate(Date sentDate) throws MessagingException {
        setHeader("Date", mDateFormat.format(sentDate));
        this.mSentDate = sentDate;
    }

    public String getContentType() throws MessagingException {
        String contentType = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType == null) {
            return "text/plain";
        } else {
            return contentType;
        }
    }

    public String getDisposition() throws MessagingException {
        String contentDisposition = getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION);
        if (contentDisposition == null) {
            return null;
        } else {
            return contentDisposition;
        }
    }

    public String getMimeType() throws MessagingException {
        return MimeUtility.getHeaderParameter(getContentType(), null);
    }

    public int getSize() throws MessagingException {
        return mSize;
    }

    /**
     * Returns a list of the given recipient type from this message. If no addresses are
     * found the method returns an empty array.
     */
    public Address[] getRecipients(RecipientType type) throws MessagingException {
        if (type == RecipientType.TO) {
            if (mTo == null) {
                mTo = Address.parse(MimeUtility.unfold(getFirstHeader("To")));
            }
            return mTo;
        } else if (type == RecipientType.CC) {
            if (mCc == null) {
                mCc = Address.parse(MimeUtility.unfold(getFirstHeader("CC")));
            }
            return mCc;
        } else if (type == RecipientType.BCC) {
            if (mBcc == null) {
                mBcc = Address.parse(MimeUtility.unfold(getFirstHeader("BCC")));
            }
            return mBcc;
        } else {
            throw new MessagingException("Unrecognized recipient type.");
        }
    }

    public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
        if (type == RecipientType.TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("To");
                this.mTo = null;
            } else {
                setHeader("To", Address.toString(addresses));
                this.mTo = addresses;
            }
        } else if (type == RecipientType.CC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("CC");
                this.mCc = null;
            } else {
                setHeader("CC", Address.toString(addresses));
                this.mCc = addresses;
            }
        } else if (type == RecipientType.BCC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("BCC");
                this.mBcc = null;
            } else {
                setHeader("BCC", Address.toString(addresses));
                this.mBcc = addresses;
            }
        } else {
            throw new MessagingException("Unrecognized recipient type.");
        }
    }

    /**
     * Returns the unfolded, decoded value of the Subject header.
     */
    public String getSubject() throws MessagingException {
        return MimeUtility.unfoldAndDecode(getFirstHeader("Subject"));
    }

    public void setSubject(String subject) throws MessagingException {
        setHeader("Subject", subject);
    }

    public Address[] getFrom() throws MessagingException {
        if (mFrom == null) {
            String list = MimeUtility.unfold(getFirstHeader("From"));
            if (list == null || list.length() == 0) {
                list = MimeUtility.unfold(getFirstHeader("Sender"));
            }
            mFrom = Address.parse(list);
        }
        return mFrom;
    }

    public void setFrom(Address from) throws MessagingException {
        if (from != null) {
            setHeader("From", from.toString());
            this.mFrom = new Address[] {
                    from
                };
        } else {
            this.mFrom = null;
        }
    }

    public Address[] getReplyTo() throws MessagingException {
        if (mReplyTo == null) {
            mReplyTo = Address.parse(MimeUtility.unfold(getFirstHeader("Reply-to")));
        }
        return mReplyTo;
    }

    public void setReplyTo(Address[] replyTo) throws MessagingException {
        if (replyTo == null || replyTo.length == 0) {
            removeHeader("Reply-to");
            mReplyTo = null;
        } else {
            setHeader("Reply-to", Address.toString(replyTo));
            mReplyTo = replyTo;
        }
    }

    public void saveChanges() throws MessagingException {
        throw new MessagingException("saveChanges not yet implemented");
    }

    public Body getBody() throws MessagingException {
        return mBody;
    }

    public void setBody(Body body) throws MessagingException {
        this.mBody = body;
        if (body instanceof com.android.email.mail.Multipart) {
            com.android.email.mail.Multipart multipart = ((com.android.email.mail.Multipart)body);
            multipart.setParent(this);
            setHeader(MimeHeader.HEADER_CONTENT_TYPE, multipart.getContentType());
            setHeader("MIME-Version", "1.0");
        }
        else if (body instanceof TextBody) {
            setHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\n charset=utf-8",
                    getMimeType()));
            setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");
        }
    }

    protected String getFirstHeader(String name) throws MessagingException {
        return mHeader.getFirstHeader(name);
    }

    public void addHeader(String name, String value) throws MessagingException {
        mHeader.addHeader(name, value);
    }

    public void setHeader(String name, String value) throws MessagingException {
        mHeader.setHeader(name, value);
    }

    public String[] getHeader(String name) throws MessagingException {
        return mHeader.getHeader(name);
    }

    public void removeHeader(String name) throws MessagingException {
        mHeader.removeHeader(name);
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        mHeader.writeTo(out);
        writer.write("\r\n");
        writer.flush();
        if (mBody != null) {
            mBody.writeTo(out);
        }
    }

    public InputStream getInputStream() throws MessagingException {
        return null;
    }

    class MimeMessageBuilder implements ContentHandler {
        private Stack stack = new Stack();

        public MimeMessageBuilder() {
        }

        private void expect(Class c) {
            if (!c.isInstance(stack.peek())) {
                throw new IllegalStateException("Internal stack error: " + "Expected '"
                        + c.getName() + "' found '" + stack.peek().getClass().getName() + "'");
            }
        }

        public void startMessage() {
            if (stack.isEmpty()) {
                stack.push(MimeMessage.this);
            } else {
                expect(Part.class);
                try {
                    MimeMessage m = new MimeMessage();
                    ((Part)stack.peek()).setBody(m);
                    stack.push(m);
                } catch (MessagingException me) {
                    throw new Error(me);
                }
            }
        }

        public void endMessage() {
            expect(MimeMessage.class);
            stack.pop();
        }

        public void startHeader() {
            expect(Part.class);
        }

        public void field(String fieldData) {
            expect(Part.class);
            try {
                String[] tokens = fieldData.split(":", 2);
                ((Part)stack.peek()).addHeader(tokens[0], tokens[1].trim());
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        public void endHeader() {
            expect(Part.class);
        }

        public void startMultipart(BodyDescriptor bd) {
            expect(Part.class);

            Part e = (Part)stack.peek();
            try {
                MimeMultipart multiPart = new MimeMultipart(e.getContentType());
                e.setBody(multiPart);
                stack.push(multiPart);
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        public void body(BodyDescriptor bd, InputStream in) throws IOException {
            expect(Part.class);
            Body body = MimeUtility.decodeBody(in, bd.getTransferEncoding());
            try {
                ((Part)stack.peek()).setBody(body);
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        public void endMultipart() {
            stack.pop();
        }

        public void startBodyPart() {
            expect(MimeMultipart.class);

            try {
                MimeBodyPart bodyPart = new MimeBodyPart();
                ((MimeMultipart)stack.peek()).addBodyPart(bodyPart);
                stack.push(bodyPart);
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        public void endBodyPart() {
            expect(BodyPart.class);
            stack.pop();
        }

        public void epilogue(InputStream is) throws IOException {
            expect(MimeMultipart.class);
            StringBuffer sb = new StringBuffer();
            int b;
            while ((b = is.read()) != -1) {
                sb.append((char)b);
            }
            // ((Multipart) stack.peek()).setEpilogue(sb.toString());
        }

        public void preamble(InputStream is) throws IOException {
            expect(MimeMultipart.class);
            StringBuffer sb = new StringBuffer();
            int b;
            while ((b = is.read()) != -1) {
                sb.append((char)b);
            }
            try {
                ((MimeMultipart)stack.peek()).setPreamble(sb.toString());
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        public void raw(InputStream is) throws IOException {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
