/**
 *
 */

package com.fsck.k9.mail.store;

import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.FixedLengthInputStream;
import com.fsck.k9.PeekableInputStream;
import com.fsck.k9.mail.MessagingException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ImapResponseParser
{
    SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);

    SimpleDateFormat badDateTimeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);
    SimpleDateFormat badDateTimeFormat2 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);

    PeekableInputStream mIn;
    InputStream mActiveLiteral;

    public ImapResponseParser(PeekableInputStream in)
    {
        this.mIn = in;
    }

    /**
     * Reads the next response available on the stream and returns an
     * ImapResponse object that represents it.
     *
     * @return
     * @throws IOException
     */
    public ImapResponse readResponse() throws IOException
    {
        ImapResponse response = new ImapResponse();
        if (mActiveLiteral != null)
        {
            while (mActiveLiteral.read() != -1)
                ;
            mActiveLiteral = null;
        }
        int ch = mIn.peek();
        if (ch == '*')
        {
            parseUntaggedResponse();
            readTokens(response);
        }
        else if (ch == '+')
        {
            response.mCommandContinuationRequested =
                parseCommandContinuationRequest();
            readTokens(response);
        }
        else
        {
            response.mTag = parseTaggedResponse();
            readTokens(response);
        }
        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "<<< " + response.toString());
        }
        return response;
    }

    private void readTokens(ImapResponse response) throws IOException
    {
        response.clear();
        Object token;
        while ((token = readToken()) != null)
        {
            if (response != null)
            {
                response.add(token);
            }
            if (mActiveLiteral != null)
            {
                break;
            }
        }
        response.mCompleted = token == null;
    }

    /**
     * Reads the next token of the response. The token can be one of: String -
     * for NIL, QUOTED, NUMBER, ATOM. InputStream - for LITERAL.
     * InputStream.available() returns the total length of the stream.
     * ImapResponseList - for PARENTHESIZED LIST. Can contain any of the above
     * elements including List.
     *
     * @return The next token in the response or null if there are no more
     *         tokens.
     * @throws IOException
     */
    public Object readToken() throws IOException
    {
        while (true)
        {
            Object token = parseToken();
            if (token == null || !token.equals(")") || !token.equals("]"))
            {
                return token;
            }
        }
    }

    private Object parseToken() throws IOException
    {
        if (mActiveLiteral != null)
        {
            while (mActiveLiteral.read() != -1)
                ;
            mActiveLiteral = null;
        }
        while (true)
        {
            int ch = mIn.peek();
            if (ch == '(')
            {
                return parseList();
            }
            else if (ch == '[')
            {
                return parseSequence();
            }
            else if (ch == ')')
            {
                expect(')');
                return ")";
            }
            else if (ch == ']')
            {
                expect(']');
                return "]";
            }
            else if (ch == '"')
            {
                return parseQuoted();
            }
            else if (ch == '{')
            {
                mActiveLiteral = parseLiteral();
                return mActiveLiteral;
            }
            else if (ch == ' ')
            {
                expect(' ');
            }
            else if (ch == '\r')
            {
                expect('\r');
                expect('\n');
                return null;
            }
            else if (ch == '\n')
            {
                expect('\n');
                return null;
            }
            else if (ch == '\t')
            {
                expect('\t');
            }
            else
            {
                return parseAtom();
            }
        }
    }

    private boolean parseCommandContinuationRequest() throws IOException
    {
        expect('+');
        expect(' ');
        return true;
    }

    // * OK [UIDNEXT 175] Predicted next UID
    private void parseUntaggedResponse() throws IOException
    {
        expect('*');
        expect(' ');
    }

    // 3 OK [READ-WRITE] Select completed.
    private String parseTaggedResponse() throws IOException
    {
        String tag = readStringUntil(' ');
        return tag;
    }

    private ImapList parseList() throws IOException
    {
        expect('(');
        ImapList list = new ImapList();
        Object token;
        while (true)
        {
            token = parseToken();
            if (token == null)
            {
                break;
            }
            else if (token instanceof InputStream)
            {
                list.add(token);
                break;
            }
            else if (token.equals(")"))
            {
                break;
            }
            else
            {
                list.add(token);
            }
        }
        return list;
    }

    private ImapList parseSequence() throws IOException
    {
        expect('[');
        ImapList list = new ImapList();
        Object token;
        while (true)
        {
            token = parseToken();
            if (token == null)
            {
                break;
            }
            else if (token instanceof InputStream)
            {
                list.add(token);
                break;
            }
            else if (token.equals("]"))
            {
                break;
            }
            else
            {
                list.add(token);
            }
        }
        return list;
    }

    private String parseAtom() throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch;
        while (true)
        {
            ch = mIn.peek();
            if (ch == -1)
            {
                throw new IOException("parseAtom(): end of stream reached");
            }
            else if (ch == '(' || ch == ')' || ch == '{' || ch == ' ' ||
                     ch == '[' || ch == ']' ||
                     // docs claim that flags are \ atom but atom isn't supposed to
                     // contain
                     // * and some falgs contain *
                     // ch == '%' || ch == '*' ||
//                    ch == '%' ||
                     // TODO probably should not allow \ and should recognize
                     // it as a flag instead
                     // ch == '"' || ch == '\' ||
                     ch == '"' || (ch >= 0x00 && ch <= 0x1f) || ch == 0x7f)
            {
                if (sb.length() == 0)
                {
                    throw new IOException(String.format("parseAtom(): (%04x %c)", (int)ch, ch));
                }
                return sb.toString();
            }
            else
            {
                sb.append((char)mIn.read());
            }
        }
    }

    /**
     * A { has been read, read the rest of the size string, the space and then
     * notify the listener with an InputStream.
     *
     * @param mListener
     * @throws IOException
     */
    private InputStream parseLiteral() throws IOException
    {
        expect('{');
        int size = Integer.parseInt(readStringUntil('}'));
        expect('\r');
        expect('\n');
        FixedLengthInputStream fixed = new FixedLengthInputStream(mIn, size);
        return fixed;
    }

    /**
     * A " has been read, read to the end of the quoted string and notify the
     * listener.
     *
     * @param mListener
     * @throws IOException
     */
    private String parseQuoted() throws IOException
    {
        expect('"');
        return readStringUntil('"');
    }

    private String readStringUntil(char end) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = mIn.read()) != -1)
        {
            if (ch == end)
            {
                return sb.toString();
            }
            else
            {
                sb.append((char)ch);
            }
        }
        throw new IOException("readQuotedString(): end of stream reached");
    }

    private int expect(char ch) throws IOException
    {
        int d;
        if ((d = mIn.read()) != ch)
        {
            throw new IOException(String.format("Expected %04x (%c) but got %04x (%c)", (int)ch,
                                                ch, d, (char)d));
        }
        return d;
    }

    /**
     * Represents an IMAP LIST response and is also the base class for the
     * ImapResponse.
     */
    public class ImapList extends ArrayList<Object>
    {
        public ImapList getList(int index)
        {
            return (ImapList)get(index);
        }

        public Object getObject(int index)
        {
            return get(index);
        }

        public String getString(int index)
        {
            return (String)get(index);
        }

        public InputStream getLiteral(int index)
        {
            return (InputStream)get(index);
        }

        public int getNumber(int index)
        {
            return Integer.parseInt(getString(index));
        }

        public Date getDate(int index) throws MessagingException
        {
            try
            {
                return parseDate(getString(index));
            }
            catch (ParseException pe)
            {
                throw new MessagingException("Unable to parse IMAP datetime", pe);
            }
        }

        public Object getKeyedValue(Object key)
        {
            for (int i = 0, count = size(); i < count; i++)
            {
                if (get(i).equals(key))
                {
                    return get(i + 1);
                }
            }
            return null;
        }

        public ImapList getKeyedList(Object key)
        {
            return (ImapList)getKeyedValue(key);
        }

        public String getKeyedString(Object key)
        {
            return (String)getKeyedValue(key);
        }

        public InputStream getKeyedLiteral(Object key)
        {
            return (InputStream)getKeyedValue(key);
        }

        public int getKeyedNumber(Object key)
        {
            return Integer.parseInt(getKeyedString(key));
        }

        public Date getKeyedDate(Object key) throws MessagingException
        {
            try
            {
                String value = getKeyedString(key);
                if (value == null)
                {
                    return null;
                }
                return parseDate(value);
            }
            catch (ParseException pe)
            {
                throw new MessagingException("Unable to parse IMAP datetime", pe);
            }
        }
        
        public boolean containsKey(Object key)
        {
            if (key == null)
            {
                return false;
            }
            
            for (int i = 0, count = size(); i < count; i++)
            {
                if (key.equals(get(i)))
                {
                    return true;
                }
            }
            return false;
        }
        
        public int getKeyIndex(Object key)
        {
            for (int i = 0, count = size(); i < count; i++)
            {
                if (key.equals(get(i)))
                {
                    return i;
                }
            }
            
            throw new IllegalArgumentException("getKeyIndex() only works for keys that are in the collection.");
        }

        private Date parseDate(String value) throws ParseException
        {
            //TODO: clean this up a bit
            try
            {
                synchronized (mDateTimeFormat)
                {
                    return mDateTimeFormat.parse(value);
                }
            }
            catch (Exception e)
            {
                try
                {
                    synchronized (badDateTimeFormat)
                    {
                        return badDateTimeFormat.parse(value);
                    }
                }
                catch (Exception e2)
                {
                    synchronized (badDateTimeFormat2)
                    {
                        return badDateTimeFormat2.parse(value);
                    }
                }
            }

        }

    }

    /**
     * Represents a single response from the IMAP server. Tagged responses will
     * have a non-null tag. Untagged responses will have a null tag. The object
     * will contain all of the available tokens at the time the response is
     * received. In general, it will either contain all of the tokens of the
     * response or all of the tokens up until the first LITERAL. If the object
     * does not contain the entire response the caller must call more() to
     * continue reading the response until more returns false.
     */
    public class ImapResponse extends ImapList
    {
        private boolean mCompleted;

        boolean mCommandContinuationRequested;
        String mTag;

        public boolean more() throws IOException
        {
            if (mCompleted)
            {
                return false;
            }
            readTokens(this);
            return true;
        }

        public String getAlertText()
        {
            if (size() > 1 && "[ALERT]".equals(get(1)))
            {
                StringBuffer sb = new StringBuffer();
                for (int i = 2, count = size(); i < count; i++)
                {
                    sb.append(get(i).toString());
                    sb.append(' ');
                }
                return sb.toString();
            }
            else
            {
                return null;
            }
        }

        @Override
        public String toString()
        {
            return "#" + (mCommandContinuationRequested ? "+" : mTag) + "# " + super.toString();
        }
    }
}
