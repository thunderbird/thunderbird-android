
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.*;
import com.fsck.k9.mail.store.UnavailableStorageException;

import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.io.EOLConvertingInputStream;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.Field;
import org.apache.james.mime4j.field.DefaultFieldParser;

import org.apache.james.mime4j.MimeException;

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
        try {
            parser.parse(new EOLConvertingInputStream(in));
        } catch (MimeException me) {
                    throw new Error(me);

        }
    }

    @Override
    public Date getSentDate()
    {
        if (mSentDate == null)
        {
            try
            {
                DateTimeField field = (DateTimeField)DefaultFieldParser.parse("Date: "
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

    @Override
    public void setSentDate(Date sentDate) throws MessagingException
    {
        removeHeader("Date");
        addSentDate(sentDate);
    }

    public void setInternalSentDate(Date sentDate)
    {
        this.mSentDate = sentDate;
    }

    @Override
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
    public String getContentId() throws MessagingException
    {
        return null;
    }
    public String getMimeType() throws MessagingException
    {
        return MimeUtility.getHeaderParameter(getContentType(), null);
    }

    public int getSize()
    {
        return mSize;
    }

    /**
     * Returns a list of the given recipient type from this message. If no addresses are
     * found the method returns an empty array.
     */
    @Override
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

    @Override
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
    @Override
    public String getSubject()
    {
        return MimeUtility.unfoldAndDecode(getFirstHeader("Subject"), this);
    }

    @Override
    public void setSubject(String subject) throws MessagingException
    {
        setHeader("Subject", subject);
    }

    @Override
    public Address[] getFrom()
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

    @Override
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

    @Override
    public Address[] getReplyTo()
    {
        if (mReplyTo == null)
        {
            mReplyTo = Address.parse(MimeUtility.unfold(getFirstHeader("Reply-to")));
        }
        return mReplyTo;
    }

    @Override
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

    @Override
    public String getMessageId() throws MessagingException
    {
        if (mMessageId == null)
        {
            mMessageId = getFirstHeader("Message-ID");
        }
        if (mMessageId == null)   //  even after checking the header
        {
            setMessageId(generateMessageId());
        }
        return mMessageId;
    }

    private String generateMessageId()
    {
        return "<"+UUID.randomUUID().toString()+"@email.android.com>";
    }

    public void setMessageId(String messageId) throws UnavailableStorageException
    {
        setHeader("Message-ID", messageId);
        mMessageId = messageId;
    }

    @Override
    public void setInReplyTo(String inReplyTo) throws MessagingException
    {
        setHeader("In-Reply-To", inReplyTo);
    }

    @Override
    public String[] getReferences() throws MessagingException
    {
        if (mReferences == null)
        {
            mReferences = getHeader("References");
        }
        return mReferences;
    }

    @Override
    public void setReferences(String references) throws MessagingException
    {
        /*
         * Make sure the References header doesn't exceed the maximum header
         * line length and won't get (Q-)encoded later. Otherwise some clients
         * will break threads apart.
         *
         * For more information see issue 1559.
         */

        // Make sure separator is SPACE to prevent Q-encoding when TAB is encountered
        references = references.replaceAll("\\s+", " ");

        /*
         * NOTE: Usually the maximum header line is 998 + CRLF = 1000 characters.
         * But at least one implementations seems to have problems with 998
         * characters, so we adjust for that fact.
         */
        final int limit = 1000 - 2 /* CRLF */ - 12 /* "References: " */ - 1 /* Off-by-one bugs */;
        final int originalLength = references.length();
        if (originalLength >= limit)
        {
            // Find start of first reference
            final int start = references.indexOf('<');

            // First reference + SPACE
            final String firstReference = references.substring(start,
                                          references.indexOf('<', start + 1));

            // Find longest tail
            final String tail = references.substring(references.indexOf('<',
                                firstReference.length() + originalLength - limit));

            references = firstReference + tail;
        }
        setHeader("References", references);
    }

    @Override
    public void saveChanges() throws MessagingException
    {
        throw new MessagingException("saveChanges not yet implemented");
    }

    @Override
    public Body getBody()
    {
        return mBody;
    }

    @Override
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
            setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "quoted-printable");
        }
    }

    protected String getFirstHeader(String name)
    {
        return mHeader.getFirstHeader(name);
    }

    @Override
    public void addHeader(String name, String value) throws UnavailableStorageException
    {
        mHeader.addHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) throws UnavailableStorageException
    {
        mHeader.setHeader(name, value);
    }

    @Override
    public String[] getHeader(String name) throws UnavailableStorageException
    {
        return mHeader.getHeader(name);
    }

    @Override
    public void removeHeader(String name) throws UnavailableStorageException
    {
        mHeader.removeHeader(name);
    }

    @Override
    public Set<String> getHeaderNames() throws UnavailableStorageException
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

    @Override
    public void setEncoding(String encoding) throws UnavailableStorageException
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

    @Override
    public void setCharset(String charset) throws MessagingException
    {
        if (mBody instanceof Multipart)
        {
            ((Multipart)mBody).setCharset(charset);
        }
        else if (mBody instanceof TextBody)
        {
            MimeUtility.setCharset(charset, this);
            ((TextBody)mBody).setCharset(charset);
        }
    }

    class MimeMessageBuilder implements ContentHandler
    {
        private Stack<Object> stack = new Stack<Object>();

        public MimeMessageBuilder()
        {
        }

        private void expect(Class<?> c)
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

        public void field(RawField field)
        {
            try {
                            Field parsedField = DefaultFieldParser.parse(field.getRaw(), null);
                ((Part)stack.peek()).addHeader(parsedField.getName(), field.getBody().trim());
            } catch (MessagingException me) {
                throw new Error(me);
            } catch (MimeException me) {
                throw new Error(me);
            }
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
            ((MimeMultipart)stack.peek()).setPreamble(sb.toString());

        }

        public void raw(InputStream is) throws IOException
        {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
