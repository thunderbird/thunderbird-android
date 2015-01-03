package com.fsck.k9.mail.store.imap;

import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.FixedLengthInputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_IMAP;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_CAPABILITY;

class ImapResponseParser {
    private PeekableInputStream mIn;
    private ImapResponse mResponse;
    private Exception mException;

    public ImapResponseParser(PeekableInputStream in) {
        this.mIn = in;
    }

    public ImapResponse readResponse() throws IOException {
        return readResponse(null);
    }

    /**
     * Reads the next response available on the stream and returns an
     * ImapResponse object that represents it.
     */
    public ImapResponse readResponse(ImapResponseCallback callback) throws IOException {
        try {
            int ch = mIn.peek();
            if (ch == '*') {
                parseUntaggedResponse();
                mResponse = new ImapResponse(callback, false, null);
                readTokens(mResponse);
            } else if (ch == '+') {
                mResponse = new ImapResponse(callback, parseCommandContinuationRequest(), null);
                parseResponseText(mResponse);
            } else {
                mResponse = new ImapResponse(callback, false, parseTaggedResponse());
                readTokens(mResponse);
            }

            if (mException != null) {
                throw new RuntimeException("readResponse(): Exception in callback method", mException);
            }

            return mResponse;
        } finally {
            mResponse = null;
            mException = null;
        }
    }

    protected List<ImapResponse> readStatusResponse(String tag,
                                                    String commandToLog,
                                                    String logId,
                                                    UntaggedHandler untaggedHandler)
            throws IOException, MessagingException {
        List<ImapResponse> responses = new ArrayList<ImapResponse>();
        ImapResponse response;
        do {
            response = readResponse();
            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                Log.v(LOG_TAG, logId + "<<<" + response);
            }

            if (response.getTag() != null && !response.getTag().equalsIgnoreCase(tag)) {
                Log.w(LOG_TAG, "After sending tag " + tag + ", got tag response from previous command " + response + " for " + logId);

                Iterator<ImapResponse> iter = responses.iterator();

                while (iter.hasNext()) {
                    ImapResponse delResponse = iter.next();
                    if (delResponse.getTag() != null
                            || delResponse.size() < 2
                            || (!equalsIgnoreCase(delResponse.get(1), "EXISTS") &&
                                !equalsIgnoreCase(delResponse.get(1), "EXPUNGE"))) {
                        iter.remove();
                    }
                }
                response = null;
                continue;
            }
            if (untaggedHandler != null) {
                untaggedHandler.handleAsyncUntaggedResponse(response);
            }
            responses.add(response);

        } while (response == null || response.getTag() == null);

        if (response.size() < 1 || !equalsIgnoreCase(response.get(0), "OK")) {
            throw new ImapException("Command: " + commandToLog + "; response: " + response.toString(), response.getAlertText());
        }

        return responses;
    }

    protected static Set<String> parseCapabilities(List<ImapResponse> responses) {
        HashSet<String> capabilities = new HashSet<String>();
        for (ImapResponse response : responses) {
            ImapList list = null;
            if (!response.isEmpty() && equalsIgnoreCase(response.get(0), "OK")) {
                for (Object thisPart : response) {
                    if (thisPart instanceof ImapList) {
                        ImapList thisList = (ImapList)thisPart;
                        if (equalsIgnoreCase(thisList.get(0), CAPABILITY_CAPABILITY)) {
                            list = thisList;
                            break;
                        }
                    }
                }
            } else if (response.getTag() == null) {
                list = response;
            }

            if (list != null && list.size() > 1 &&
                 equalsIgnoreCase(list.get(0), CAPABILITY_CAPABILITY)) {
                for (Object capability : list.subList(1, list.size())) {
                    if (capability instanceof String) {
                        capabilities.add(((String)capability).toUpperCase(Locale.US));
                    }
                }
            }
        }
        return capabilities;
    }

    private void readTokens(ImapResponse response) throws IOException {
        response.clear();

        String firstToken = (String) readToken(response);
        response.add(firstToken);

        if (isStatusResponse(firstToken)) {
            parseResponseText(response);
        } else {
            Object token;
            while ((token = readToken(response)) != null) {
                if (!(token instanceof ImapList)) {
                    response.add(token);
                }
            }
        }
    }

    /**
     * Parse {@code resp-text} tokens
     *
     * <p>
     * Responses "OK", "PREAUTH", "BYE", "NO", "BAD", and continuation request responses can
     * contain {@code resp-text} tokens. We parse the {@code resp-text-code} part as tokens and
     * read the rest as sequence of characters to avoid the parser interpreting things like
     * "{123}" as start of a literal.
     * </p>
     * <p>Example:</p>
     * <p>
     * {@code * OK [UIDVALIDITY 3857529045] UIDs valid}
     * </p>
     * <p>
     * See RFC 3501, Section 9 Formal Syntax (resp-text)
     * </p>
     *
     * @param parent
     *         The {@link ImapResponse} instance that holds the parsed tokens of the response.
     *
     * @throws IOException
     *          If there's a network error.
     *
     * @see #isStatusResponse(String)
     */
    private void parseResponseText(ImapResponse parent) throws IOException {
        skipIfSpace();

        int next = mIn.peek();
        if (next == '[') {
            parseSequence(parent);
            skipIfSpace();
        }

        String rest = readStringUntil('\r');
        expect('\n');

        if (!TextUtils.isEmpty(rest)) {
            // The rest is free-form text.
            parent.add(rest);
        }
    }

    private void skipIfSpace() throws IOException {
        if (mIn.peek() == ' ') {
            expect(' ');
        }
    }

    /**
     * Reads the next token of the response. The token can be one of: String -
     * for NIL, QUOTED, NUMBER, ATOM. Object - for LITERAL.
     * ImapList - for PARENTHESIZED LIST. Can contain any of the above
     * elements including List.
     *
     * @return The next token in the response or null if there are no more
     *         tokens.
     */
    private Object readToken(ImapResponse response) throws IOException {
        while (true) {
            Object token = parseToken(response);
            if (token == null || !(token.equals(")") || token.equals("]"))) {
                return token;
            }
        }
    }

    private Object parseToken(ImapList parent) throws IOException {
        while (true) {
            int ch = mIn.peek();
            if (ch == '(') {
                return parseList(parent);
            } else if (ch == '[') {
                return parseSequence(parent);
            } else if (ch == ')') {
                expect(')');
                return ")";
            } else if (ch == ']') {
                expect(']');
                return "]";
            } else if (ch == '"') {
                return parseQuoted();
            } else if (ch == '{') {
                return parseLiteral();
            } else if (ch == ' ') {
                expect(' ');
            } else if (ch == '\r') {
                expect('\r');
                expect('\n');
                return null;
            } else if (ch == '\n') {
                expect('\n');
                return null;
            } else if (ch == '\t') {
                expect('\t');
            } else {
                return parseAtom();
            }
        }
    }

    private boolean parseCommandContinuationRequest() throws IOException {
        expect('+');
        return true;
    }

    // * OK [UIDNEXT 175] Predicted next UID
    private void parseUntaggedResponse() throws IOException {
        expect('*');
        expect(' ');
    }

    // 3 OK [READ-WRITE] Select completed.
    private String parseTaggedResponse() throws IOException {
        return readStringUntil(' ');
    }

    private ImapList parseList(ImapList parent) throws IOException {
        expect('(');
        ImapList list = new ImapList();
        parent.add(list);
        Object token;
        while (true) {
            token = parseToken(list);
            if (token == null) {
                return null;
            } else if (token.equals(")")) {
                break;
            } else if (token instanceof ImapList) {
                // Do nothing
            } else {
                list.add(token);
            }
        }
        return list;
    }

    private ImapList parseSequence(ImapList parent) throws IOException {
        expect('[');
        ImapList list = new ImapList();
        parent.add(list);
        Object token;
        while (true) {
            token = parseToken(list);
            if (token == null) {
                return null;
            } else if (token.equals("]")) {
                break;
            } else if (token instanceof ImapList) {
                // Do nothing
            } else {
                list.add(token);
            }
        }
        return list;
    }

    private String parseAtom() throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while (true) {
            ch = mIn.peek();
            if (ch == -1) {
                throw new IOException("parseAtom(): end of stream reached");
            } else if (ch == '(' || ch == ')' || ch == '{' || ch == ' ' ||
                       ch == '[' || ch == ']' ||
                       // docs claim that flags are \ atom but atom isn't supposed to
                       // contain
                       // * and some flags contain *
                       // ch == '%' || ch == '*' ||
//                    ch == '%' ||
                       // TODO probably should not allow \ and should recognize
                       // it as a flag instead
                       // ch == '"' || ch == '\' ||
                       ch == '"' || (ch >= 0x00 && ch <= 0x1f) || ch == 0x7f) {
                if (sb.length() == 0) {
                    throw new IOException(String.format("parseAtom(): (%04x %c)", ch, ch));
                }
                return sb.toString();
            } else {
                sb.append((char)mIn.read());
            }
        }
    }

    /**
     * A "{" has been read. Read the rest of the size string, the space and then
     * notify the callback with an InputStream.
     */
    private Object parseLiteral() throws IOException {
        expect('{');
        int size = Integer.parseInt(readStringUntil('}'));
        expect('\r');
        expect('\n');

        if (size == 0) {
            return "";
        }

        if (mResponse.getCallback() != null) {
            FixedLengthInputStream fixed = new FixedLengthInputStream(mIn, size);

            Object result = null;
            try {
                result = mResponse.getCallback().foundLiteral(mResponse, fixed);
            } catch (IOException e) {
                // Pass IOExceptions through
                throw e;
            } catch (Exception e) {
                // Catch everything else and save it for later.
                mException = e;
                //Log.e(LOG_TAG, "parseLiteral(): Exception in callback method", e);
            }

            // Check if only some of the literal data was read
            int available = fixed.available();
            if ((available > 0) && (available != size)) {
                // If so, skip the rest
                while (fixed.available() > 0) {
                    fixed.skip(fixed.available());
                }
            }

            if (result != null) {
                return result;
            }
        }

        byte[] data = new byte[size];
        int read = 0;
        while (read != size) {
            int count = mIn.read(data, read, size - read);
            if (count == -1) {
                throw new IOException("parseLiteral(): end of stream reached");
            }
            read += count;
        }

        return new String(data, "US-ASCII");
    }

    private String parseQuoted() throws IOException {
        expect('"');

        StringBuilder sb = new StringBuilder();
        int ch;
        boolean escape = false;
        while ((ch = mIn.read()) != -1) {
            if (!escape && (ch == '\\')) {
                // Found the escape character
                escape = true;
            } else if (!escape && (ch == '"')) {
                return sb.toString();
            } else {
                sb.append((char)ch);
                escape = false;
            }
        }
        throw new IOException("parseQuoted(): end of stream reached");
    }

    private String readStringUntil(char end) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = mIn.read()) != -1) {
            if (ch == end) {
                return sb.toString();
            } else {
                sb.append((char)ch);
            }
        }
        throw new IOException("readStringUntil(): end of stream reached");
    }

    private int expect(char ch) throws IOException {
        int d;
        if ((d = mIn.read()) != ch) {
            throw new IOException(String.format("Expected %04x (%c) but got %04x (%c)", (int)ch,
                                                ch, d, (char)d));
        }
        return d;
    }

    public boolean isStatusResponse(String symbol) {
        return symbol.equalsIgnoreCase("OK") ||
               symbol.equalsIgnoreCase("NO") ||
               symbol.equalsIgnoreCase("BAD") ||
               symbol.equalsIgnoreCase("PREAUTH") ||
               symbol.equalsIgnoreCase("BYE");
    }

    public static boolean equalsIgnoreCase(Object o1, Object o2) {
        if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
            String s1 = (String)o1;
            String s2 = (String)o2;
            return s1.equalsIgnoreCase(s2);
        } else if (o1 != null) {
            return o1.equals(o2);
        } else if (o2 != null) {
            return o2.equals(o1);
        } else {
            // Both o1 and o2 are null
            return true;
        }
    }

}
