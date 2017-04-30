package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.filter.FixedLengthInputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import timber.log.Timber;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_IMAP;


class ImapResponseParser {
    private PeekableInputStream inputStream;
    private ImapResponse response;
    private Exception exception;


    public ImapResponseParser(PeekableInputStream in) {
        this.inputStream = in;
    }

    public ImapResponse readResponse() throws IOException {
        return readResponse(null);
    }

    /**
     * Reads the next response available on the stream and returns an {@code ImapResponse} object that represents it.
     */
    public ImapResponse readResponse(ImapResponseCallback callback) throws IOException {
        try {
            int peek = inputStream.peek();
            if (peek == '+') {
                readContinuationRequest(callback);
            } else if (peek == '*') {
                readUntaggedResponse(callback);
            } else {
                readTaggedResponse(callback);
            }

            if (exception != null) {
                throw new ImapResponseParserException("readResponse(): Exception in callback method", exception);
            }

            return response;
        } finally {
            response = null;
            exception = null;
        }
    }

    private void readContinuationRequest(ImapResponseCallback callback) throws IOException {
        parseCommandContinuationRequest();
        response = ImapResponse.newContinuationRequest(callback);

        skipIfSpace();
        String rest = readStringUntilEndOfLine();
        response.add(rest);
    }

    private void readUntaggedResponse(ImapResponseCallback callback) throws IOException {
        parseUntaggedResponse();
        response = ImapResponse.newUntaggedResponse(callback);

        readTokens(response);
    }

    private void readTaggedResponse(ImapResponseCallback callback) throws IOException {
        String tag = parseTaggedResponse();
        response = ImapResponse.newTaggedResponse(callback, tag);

        readTokens(response);
    }

    List<ImapResponse> readStatusResponse(String tag, String commandToLog, String logId,
            UntaggedHandler untaggedHandler) throws IOException, NegativeImapResponseException {

        List<ImapResponse> responses = new ArrayList<ImapResponse>();

        ImapResponse response;
        do {
            response = readResponse();

            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                Timber.v("%s<<<%s", logId, response);
            }

            if (response.getTag() != null && !response.getTag().equalsIgnoreCase(tag)) {
                Timber.w("After sending tag %s, got tag response from previous command %s for %s", tag, response, logId);

                Iterator<ImapResponse> responseIterator = responses.iterator();

                while (responseIterator.hasNext()) {
                    ImapResponse delResponse = responseIterator.next();
                    if (delResponse.getTag() != null || delResponse.size() < 2 || (
                            !equalsIgnoreCase(delResponse.get(1), Responses.EXISTS) &&
                            !equalsIgnoreCase(delResponse.get(1), Responses.EXPUNGE))) {
                        responseIterator.remove();
                    }
                }
                response = null;
                continue;
            }

            if (response.getTag() == null && untaggedHandler != null) {
                untaggedHandler.handleAsyncUntaggedResponse(response);
            }

            responses.add(response);
        } while (response == null || response.getTag() == null);

        if (response.size() < 1 || !equalsIgnoreCase(response.get(0), Responses.OK)) {
            String message = "Command: " + commandToLog + "; response: " + response.toString();
            throw new NegativeImapResponseException(message, responses);
        }

        return responses;
    }

    private void readTokens(ImapResponse response) throws IOException {
        response.clear();

        Object firstToken = readToken(response);

        checkTokenIsString(firstToken);
        String symbol = (String) firstToken;

        response.add(symbol);

        if (isStatusResponse(symbol)) {
            parseResponseText(response);
        } else if (equalsIgnoreCase(symbol, Responses.LIST) || equalsIgnoreCase(symbol, Responses.LSUB)) {
            parseListResponse(response);
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
     *         If there's a network error.
     *
     * @see #isStatusResponse(String)
     */
    private void parseResponseText(ImapResponse parent) throws IOException {
        skipIfSpace();

        int next = inputStream.peek();
        if (next == '[') {
            parseList(parent, '[', ']');
            skipIfSpace();
        }

        String rest = readStringUntilEndOfLine();

        if (rest != null && !rest.isEmpty()) {
            // The rest is free-form text.
            parent.add(rest);
        }
    }

    private void parseListResponse(ImapResponse response) throws IOException {
        expect(' ');
        parseList(response, '(', ')');
        expect(' ');
        String delimiter = parseQuotedOrNil();
        response.add(delimiter);
        expect(' ');
        String name = parseString();
        response.add(name);
        expect('\r');
        expect('\n');
    }

    private void skipIfSpace() throws IOException {
        if (inputStream.peek() == ' ') {
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
     * tokens.
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
            int ch = inputStream.peek();

            if (ch == '(') {
                return parseList(parent, '(', ')');
            } else if (ch == '[') {
                return parseList(parent, '[', ']');
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
                return parseBareString(true);
            }
        }
    }

    private String parseString() throws IOException {
        int ch = inputStream.peek();

        if (ch == '"') {
            return parseQuoted();
        } else if (ch == '{') {
            return (String) parseLiteral();
        } else {
            return parseBareString(false);
        }
    }

    private boolean parseCommandContinuationRequest() throws IOException {
        expect('+');
        return true;
    }

    private void parseUntaggedResponse() throws IOException {
        expect('*');
        expect(' ');
    }

    private String parseTaggedResponse() throws IOException {
        return readStringUntil(' ');
    }

    private ImapList parseList(ImapList parent, char start, char end) throws IOException {
        expect(start);

        ImapList list = new ImapList();
        parent.add(list);

        String endString = String.valueOf(end);

        Object token;
        while (true) {
            token = parseToken(list);
            if (token == null) {
                return null;
            } else if (token.equals(endString)) {
                break;
            } else if (!(token instanceof ImapList)) {
                list.add(token);
            }
        }

        return list;
    }

    private String parseBareString(boolean allowBrackets) throws IOException {
        StringBuilder sb = new StringBuilder();

        int ch;
        while (true) {
            ch = inputStream.peek();
            if (ch == -1) {
                throw new IOException("parseBareString(): end of stream reached");
            }

            if (ch == '(' || ch == ')' || (allowBrackets && (ch == '[' || ch == ']')) ||
                    ch == '{' || ch == ' ' || ch == '"' ||
                    (ch >= 0x00 && ch <= 0x1f) || ch == 0x7f) {

                if (sb.length() == 0) {
                    throw new IOException(String.format("parseBareString(): (%04x %c)", ch, ch));
                }

                return sb.toString();
            } else {
                sb.append((char) inputStream.read());
            }
        }
    }

    /**
     * A "{" has been read. Read the rest of the size string, the space and then notify the callback with an
     * {@code InputStream}.
     */
    private Object parseLiteral() throws IOException {
        expect('{');
        int size = Integer.parseInt(readStringUntil('}'));
        expect('\r');
        expect('\n');

        if (size == 0) {
            return "";
        }

        if (response.getCallback() != null) {
            FixedLengthInputStream fixed = new FixedLengthInputStream(inputStream, size);

            Exception callbackException = null;
            Object result = null;
            try {
                result = response.getCallback().foundLiteral(response, fixed);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                callbackException = e;
            }

            boolean someDataWasRead = fixed.available() != size;
            if (someDataWasRead) {
                if (result == null && callbackException == null) {
                    throw new AssertionError("Callback consumed some data but returned no result");
                }

                fixed.skipRemaining();
            }

            if (callbackException != null) {
                if (exception == null) {
                    exception = callbackException;
                }
                return "EXCEPTION";
            }
            
            if (result != null) {
                return result;
            }
        }

        byte[] data = new byte[size];
        int read = 0;
        while (read != size) {
            int count = inputStream.read(data, read, size - read);
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
        while ((ch = inputStream.read()) != -1) {
            if (!escape && ch == '\\') {
                // Found the escape character
                escape = true;
            } else if (!escape && ch == '"') {
                return sb.toString();
            } else {
                sb.append((char) ch);
                escape = false;
            }
        }
        throw new IOException("parseQuoted(): end of stream reached");
    }

    private String parseQuotedOrNil() throws IOException {
        int peek = inputStream.peek();
        if (peek == '"') {
            return parseQuoted();
        } else {
            parseNil();
            return null;
        }
    }
    
    private void parseNil() throws IOException {
        expect('N');
        expect('I');
        expect('L');
    }

    private String readStringUntil(char end) throws IOException {
        StringBuilder sb = new StringBuilder();

        int ch;
        while ((ch = inputStream.read()) != -1) {
            if (ch == end) {
                return sb.toString();
            } else {
                sb.append((char) ch);
            }
        }

        throw new IOException("readStringUntil(): end of stream reached");
    }

    private String readStringUntilEndOfLine() throws IOException {
        String rest = readStringUntil('\r');
        expect('\n');

        return rest;
    }

    private void expect(char expected) throws IOException {
        int readByte = inputStream.read();
        if (readByte != expected) {
            throw new IOException(String.format("Expected %04x (%c) but got %04x (%c)",
                    (int) expected, expected, readByte, (char) readByte));
        }
    }

    private boolean isStatusResponse(String symbol) {
        return symbol.equalsIgnoreCase(Responses.OK) ||
                symbol.equalsIgnoreCase(Responses.NO) ||
                symbol.equalsIgnoreCase(Responses.BAD) ||
                symbol.equalsIgnoreCase(Responses.PREAUTH) ||
                symbol.equalsIgnoreCase(Responses.BYE);
    }

    static boolean equalsIgnoreCase(Object token, String symbol) {
        if (token == null || !(token instanceof String)) {
            return false;
        }

        return symbol.equalsIgnoreCase((String) token);
    }

    private void checkTokenIsString(Object token) throws IOException {
        if (!(token instanceof String)) {
            throw new IOException("Unexpected non-string token: " + token.getClass().getSimpleName() + " - " + token);
        }
    }
}
