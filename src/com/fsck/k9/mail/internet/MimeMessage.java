
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.*;
import org.apache.james.mime4j.BodyDescriptor;
import org.apache.james.mime4j.ContentHandler;
import org.apache.james.mime4j.EOLConvertingInputStream;
import org.apache.james.mime4j.MimeStreamParser;
import org.apache.james.mime4j.field.DateTimeField;
import org.apache.james.mime4j.field.Field;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * An implementation of Message that stores all of it's metadata in RFC 822 and
 * RFC 2045 style headers.
 */
public class MimeMessage extends Message
{
    protected MimeHeader mHeader = new MimeHeader();
    protected Address[] mFrom;
    protected Address[] mTo;
    protected Address[] mCc;
    protected Address[] mBcc;
    protected Address[] mReplyTo;

    protected String mMessageId;
    protected String[] mReferences;
    protected String[] mInReplyTo;

    protected Date mSentDate;
    protected SimpleDateFormat mDateFormat;

    protected Body mBody;
    protected int mSize;

    public MimeMessage()
    {
    }


    /**
     * Parse the given InputStream using Apache Mime4J to build a MimeMessage.
     *
     * @param in
     * @throws IOException
     * @throws MessagingException
     */
    public MimeMessage(InputStream in) throws IOException, MessagingException
    {
        parse(in);
    }

    protected void parse(InputStream in) throws IOException, MessagingException
    {
        mHeader.clear();
        mFrom = null;
        mTo = null;
        mCc = null;
        mBcc = null;
        mReplyTo = null;

        mMessageId = null;
        mReferences = null;
        mInReplyTo = null;

        mSentDate = null;

        mBody = null;

        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(new MimeMessageBuilder());
        parser.parse(new EOLConvertingInputStream(in));
    }

    public Date getReceivedDate() throws MessagingException
    {
        return null;
    }

    public Date getSentDate() throws MessagingException
    {
        if (mSentDate == null)
        {
            try
            {
                DateTimeField field = (DateTimeField)Field.parse("Date: "
                                      + MimeUtility.unfoldAndDecode(getFirstHeader("Date")));
                mSentDate = field.getDate();
            }
            catch (Exception e)
            {

            }
        }
        return mSentDate;
    }

    /**
     * Sets the sent date object member as well as *adds* the 'Date' header
     * instead of setting it (for performance reasons).
     *
     * @see #mSentDate
     * @param sentDate
     * @throws com.fsck.k9.mail.MessagingException
     */
    public void addSentDate(Date sentDate) throws MessagingException
    {
        if (mDateFormat == null)
        {
            mDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        }
        addHeader("Date", mDateFormat.format(sentDate));
        setInternalSentDate(sentDate);
    }

    public void setSentDate(Date sentDate) throws MessagingException
    {
        removeHeader("Date");
        addSentDate(sentDate);
    }

    public void setInternalSentDate(Date sentDate) throws MessagingException
    {
        this.mSentDate = sentDate;
    }

    public String getContentType() throws MessagingException
    {
        String contentType = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType == null)
        {
            return "text/plain";
        }
        else
        {
            return contentType.toLowerCase();
        }
    }

    public String getDisposition() throws MessagingException
    {
        String contentDisposition = getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION);
        if (contentDisposition == null)
        {
            return null;
        }
        else
        {
            return contentDisposition;
        }
    }

    public String getMimeType() throws MessagingException
    {
        return MimeUtility.getHeaderParameter(getContentType(), null);
    }

    public int getSize() throws MessagingException
    {
        return mSize;
    }

    /**
     * Returns a list of the given recipient type from this message. If no addresses are
     * found the method returns an empty array.
     */
    public Address[] getRecipients(RecipientType type) throws MessagingException
    {
        if (type == RecipientType.TO)
        {
            if (mTo == null)
            {
                mTo = Address.parse(MimeUtility.unfold(getFirstHeader("To")));
            }
            return mTo;
        }
        else if (type == RecipientType.CC)
        {
            if (mCc == null)
            {
                mCc = Address.parse(MimeUtility.unfold(getFirstHeader("CC")));
            }
            return mCc;
        }
        else if (type == RecipientType.BCC)
        {
            if (mBcc == null)
            {
                mBcc = Address.parse(MimeUtility.unfold(getFirstHeader("BCC")));
            }
            return mBcc;
        }
        else
        {
            throw new MessagingException("Unrecognized recipient type.");
        }
    }

    public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException
    {
        if (type == RecipientType.TO)
        {
            if (addresses == null || addresses.length == 0)
            {
                removeHeader("To");
                this.mTo = null;
            }
            else
            {
                setHeader("To", Address.toEncodedString(addresses));
                this.mTo = addresses;
            }
        }
        else if (type == RecipientType.CC)
        {
            if (addresses == null || addresses.length == 0)
            {
                removeHeader("CC");
                this.mCc = null;
            }
            else
            {
                setHeader("CC", Address.toEncodedString(addresses));
                this.mCc = addresses;
            }
        }
        else if (type == RecipientType.BCC)
        {
            if (addresses == null || addresses.length == 0)
            {
                removeHeader("BCC");
                this.mBcc = null;
            }
            else
            {
                setHeader("BCC", Address.toEncodedString(addresses));
                this.mBcc = addresses;
            }
        }
        else
        {
            throw new MessagingException("Unrecognized recipient type.");
        }
    }

    /**
     * Returns the unfolded, decoded value of the Subject header.
     */
    public String getSubject() throws MessagingException
    {
        return MimeUtility.unfoldAndDecode(getFirstHeader("Subject"));
    }

    public void setSubject(String subject) throws MessagingException
    {
        setHeader("Subject", subject);
    }

    public Address[] getFrom() throws MessagingException
    {
        if (mFrom == null)
        {
            String list = MimeUtility.unfold(getFirstHeader("From"));
            if (list == null || list.length() == 0)
            {
                list = MimeUtility.unfold(getFirstHeader("Sender"));
            }
            mFrom = Address.parse(list);
        }
        return mFrom;
    }

    public void setFrom(Address from) throws MessagingException
    {
        if (from != null)
        {
            setHeader("From", from.toEncodedString());
            this.mFrom = new Address[]
            {
                from
            };
        }
        else
        {
            this.mFrom = null;
        }
    }

    public Address[] getReplyTo() throws MessagingException
    {
        if (mReplyTo == null)
        {
            mReplyTo = Address.parse(MimeUtility.unfold(getFirstHeader("Reply-to")));
        }
        return mReplyTo;
    }

    public void setReplyTo(Address[] replyTo) throws MessagingException
    {
        if (replyTo == null || replyTo.length == 0)
        {
            removeHeader("Reply-to");
            mReplyTo = null;
        }
        else
        {
            setHeader("Reply-to", Address.toEncodedString(replyTo));
            mReplyTo = replyTo;
        }
    }

    public String getMessageId() throws MessagingException
    {
        if (mMessageId == null)
        {
            mMessageId = getFirstHeader("Message-ID");
        }
        if (mMessageId == null) //  even after checking the header
        {
            setMessageId(generateMessageId());
        }
        return mMessageId;
    }

    private String generateMessageId()
    {
        return "<"+UUID.randomUUID().toString()+"@email.android.com>";
    }

    public void setMessageId(String messageId)
    {
        setHeader("Message-ID", messageId);
        mMessageId = messageId;
    }

    public void setInReplyTo(String inReplyTo) throws MessagingException
    {
        setHeader("In-Reply-To", inReplyTo);
    }

    public String[] getReferences() throws MessagingException
    {
        if (mReferences == null)
        {
            mReferences = getHeader("References");
        }
        return mReferences;
    }

    public void setReferences(String references) throws MessagingException
    {
        setHeader("References", references);
    }

    public void saveChanges() throws MessagingException
    {
        throw new MessagingException("saveChanges not yet implemented");
    }

    public Body getBody() throws MessagingException
    {
        return mBody;
    }

    public void setBody(Body body) throws MessagingException
    {
        this.mBody = body;
        setHeader("MIME-Version", "1.0");
        if (body instanceof Multipart)
        {
            Multipart multipart = ((Multipart)body);
            multipart.setParent(this);
            setHeader(MimeHeader.HEADER_CONTENT_TYPE, multipart.getContentType());
        }
        else if (body instanceof TextBody)
        {
            setHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\n charset=utf-8",
                      getMimeType()));
            setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");
        }
    }

    protected String getFirstHeader(String name)
    {
        return mHeader.getFirstHeader(name);
    }

    public void addHeader(String name, String value)
    {
        mHeader.addHeader(name, value);
    }

    public void setHeader(String name, String value)
    {
        mHeader.setHeader(name, value);
    }

    public String[] getHeader(String name)
    {
        return mHeader.getHeader(name);
    }

    public void removeHeader(String name)
    {
        mHeader.removeHeader(name);
    }

    public List<String> getHeaderNames()
    {
        return mHeader.getHeaderNames();
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException
    {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        mHeader.writeTo(out);
        writer.write("\r\n");
        writer.flush();
        if (mBody != null)
        {
            mBody.writeTo(out);
        }
    }

    public InputStream getInputStream() throws MessagingException
    {
        return null;
    }

    public void setEncoding(String encoding)
    {
    	if (mBody instanceof Multipart)
    	{
    		((Multipart)mBody).setEncoding(encoding);
    	}
    	else if (mBody instanceof TextBody)
        {
            setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
            ((TextBody)mBody).setEncoding(encoding);
        }
    }

    class MimeMessageBuilder implements ContentHandler
    {
        private Stack stack = new Stack();

        public MimeMessageBuilder()
        {
        }

        private void expect(Class c)
        {
            if (!c.isInstance(stack.peek()))
            {
                throw new IllegalStateException("Internal stack error: " + "Expected '"
                                                + c.getName() + "' found '" + stack.peek().getClass().getName() + "'");
            }
        }

        public void startMessage()
        {
            if (stack.isEmpty())
            {
                stack.push(MimeMessage.this);
            }
            else
            {
                expect(Part.class);
                try
                {
                    MimeMessage m = new MimeMessage();
                    ((Part)stack.peek()).setBody(m);
                    stack.push(m);
                }
                catch (MessagingException me)
                {
                    throw new Error(me);
                }
            }
        }

        public void endMessage()
        {
            expect(MimeMessage.class);
            stack.pop();
        }

        public void startHeader()
        {
            expect(Part.class);
        }

        public void field(String fieldData)
        {
            expect(Part.class);
            try
            {
                String[] tokens = fieldData.split(":", 2);
                ((Part)stack.peek()).addHeader(tokens[0], tokens[1].trim());
            }
            catch (MessagingException me)
            {
                throw new Error(me);
            }
        }

        public void endHeader()
        {
            expect(Part.class);
        }

        public void startMultipart(BodyDescriptor bd)
        {
            expect(Part.class);

            Part e = (Part)stack.peek();
            try
            {
                MimeMultipart multiPart = new MimeMultipart(e.getContentType());
                e.setBody(multiPart);
                stack.push(multiPart);
            }
            catch (MessagingException me)
            {
                throw new Error(me);
            }
        }

        public void body(BodyDescriptor bd, InputStream in) throws IOException
        {
            expect(Part.class);
            Body body = MimeUtility.decodeBody(in, bd.getTransferEncoding());
            try
            {
                ((Part)stack.peek()).setBody(body);
            }
            catch (MessagingException me)
            {
                throw new Error(me);
            }
        }

        public void endMultipart()
        {
            stack.pop();
        }

        public void startBodyPart()
        {
            expect(MimeMultipart.class);

            try
            {
                MimeBodyPart bodyPart = new MimeBodyPart();
                ((MimeMultipart)stack.peek()).addBodyPart(bodyPart);
                stack.push(bodyPart);
            }
            catch (MessagingException me)
            {
                throw new Error(me);
            }
        }

        public void endBodyPart()
        {
            expect(BodyPart.class);
            stack.pop();
        }

        public void epilogue(InputStream is) throws IOException
        {
            expect(MimeMultipart.class);
            StringBuffer sb = new StringBuffer();
            int b;
            while ((b = is.read()) != -1)
            {
                sb.append((char)b);
            }
            // ((Multipart) stack.peek()).setEpilogue(sb.toString());
        }

        public void preamble(InputStream is) throws IOException
        {
            expect(MimeMultipart.class);
            StringBuffer sb = new StringBuffer();
            int b;
            while ((b = is.read()) != -1)
            {
                sb.append((char)b);
            }
            try
            {
                ((MimeMultipart)stack.peek()).setPreamble(sb.toString());
            }
            catch (MessagingException me)
            {
                throw new Error(me);
            }
        }

        public void raw(InputStream is) throws IOException
        {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
