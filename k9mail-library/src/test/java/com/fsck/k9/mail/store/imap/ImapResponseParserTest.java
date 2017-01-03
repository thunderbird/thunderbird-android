package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.filter.FixedLengthInputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ImapResponseParserTest {
    private PeekableInputStream peekableInputStream;


    @Test
    public void testSimpleOkResponse() throws IOException {
        ImapResponseParser parser = createParser("* OK\r\n");

        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("OK", response.get(0));
    }

    @Test
    public void testOkResponseWithText() throws IOException {
        ImapResponseParser parser = createParser("* OK Some text here\r\n");

        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("OK", response.get(0));
        assertEquals("Some text here", response.get(1));
    }

    @Test
    public void testOkResponseWithRespTextCode() throws IOException {
        ImapResponseParser parser = createParser("* OK [UIDVALIDITY 3857529045]\r\n");

        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("OK", response.get(0));
        assertTrue(response.get(1) instanceof ImapList);

        ImapList respTextCode = (ImapList) response.get(1);
        assertEquals(2, respTextCode.size());
        assertEquals("UIDVALIDITY", respTextCode.get(0));
        assertEquals("3857529045", respTextCode.get(1));
    }

    @Test
    public void testOkResponseWithRespTextCodeAndText() throws IOException {
        ImapResponseParser parser = createParser("* OK [token1 token2] {x} test [...]\r\n");

        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(3, response.size());
        assertEquals("OK", response.get(0));
        assertTrue(response.get(1) instanceof ImapList);
        assertEquals("{x} test [...]", response.get(2));

        ImapList respTextCode = (ImapList) response.get(1);
        assertEquals(2, respTextCode.size());
        assertEquals("token1", respTextCode.get(0));
        assertEquals("token2", respTextCode.get(1));
    }

    @Test
    public void testReadStatusResponseWithOKResponse() throws Exception {
        ImapResponseParser parser = createParser("* COMMAND BAR\tBAZ\r\nTAG OK COMMAND completed\r\n");

        List<ImapResponse> responses = parser.readStatusResponse("TAG", null, null, null);

        assertEquals(2, responses.size());
        assertEquals(asList("COMMAND", "BAR", "BAZ"), responses.get(0));
        assertEquals(asList("OK", "COMMAND completed"), responses.get(1));
    }

    @Test
    public void testReadStatusResponseSkippingWrongTag() throws Exception {
        ImapResponseParser parser = createParser("* UNTAGGED\r\n" +
                "* 0 EXPUNGE\r\n" +
                "* 42 EXISTS\r\n" +
                "A1 COMMAND BAR BAZ\r\n" +
                "A2 OK COMMAND completed\r\n");
        TestUntaggedHandler untaggedHandler = new TestUntaggedHandler();

        List<ImapResponse> responses = parser.readStatusResponse("A2", null, null, untaggedHandler);

        assertEquals(3, responses.size());
        assertEquals(asList("0", "EXPUNGE"), responses.get(0));
        assertEquals(asList("42", "EXISTS"), responses.get(1));
        assertEquals(asList("OK", "COMMAND completed"), responses.get(2));
        assertEquals(asList("UNTAGGED"), untaggedHandler.responses.get(0));
        assertEquals(responses.get(0), untaggedHandler.responses.get(1));
        assertEquals(responses.get(1), untaggedHandler.responses.get(2));
    }

    @Test(expected = NegativeImapResponseException.class)
    public void testReadStatusResponseWithErrorResponse() throws Exception {
        ImapResponseParser parser = createParser("* COMMAND BAR BAZ\r\nTAG ERROR COMMAND errored\r\n");

        parser.readStatusResponse("TAG", null, null, null);
    }

    @Test
    public void testRespTextCodeWithList() throws Exception {
        ImapResponseParser parser = createParser("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen " +
                "\\Draft NonJunk $MDNSent \\*)] Flags permitted.\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(3, response.size());
        assertTrue(response.get(1) instanceof ImapList);
        assertEquals(2, response.getList(1).size());
        assertEquals("PERMANENTFLAGS", response.getList(1).getString(0));
        assertTrue(response.getList(1).get(1) instanceof ImapList);
        assertEquals("\\Answered", response.getList(1).getList(1).getString(0));
        assertEquals("\\Flagged", response.getList(1).getList(1).getString(1));
        assertEquals("\\Deleted", response.getList(1).getList(1).getString(2));
        assertEquals("\\Seen", response.getList(1).getList(1).getString(3));
        assertEquals("\\Draft", response.getList(1).getList(1).getString(4));
        assertEquals("NonJunk", response.getList(1).getList(1).getString(5));
        assertEquals("$MDNSent", response.getList(1).getList(1).getString(6));
        assertEquals("\\*", response.getList(1).getList(1).getString(7));
    }

    @Test
    public void testExistsResponse() throws Exception {
        ImapResponseParser parser = createParser("* 23 EXISTS\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(2, response.size());
        assertEquals(23, response.getNumber(0));
        assertEquals("EXISTS", response.getString(1));
    }

    @Test(expected = IOException.class)
    public void testReadStringUntilEndOfStream() throws IOException {
        ImapResponseParser parser = createParser("* OK Some text ");

        parser.readResponse();
    }

    @Test
    public void testCommandContinuation() throws Exception {
        ImapResponseParser parser = createParser("+ Ready for additional command text\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(1, response.size());
        assertEquals("Ready for additional command text", response.getString(0));
    }

    @Test
    public void testParseLiteral() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(1, response.size());
        assertEquals("test", response.getString(0));
    }

    @Test
    public void testParseLiteralWithEmptyString() throws Exception {
        ImapResponseParser parser = createParser("* {0}\r\n\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(1, response.size());
        assertEquals("", response.getString(0));
    }

    @Test(expected = IOException.class)
    public void testParseLiteralToEndOfStream() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\nabc");

        parser.readResponse();
    }

    @Test
    public void testParseLiteralWithConsumingCallbackReturningNull() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndReturn(4, "cheeseburger");

        ImapResponse response = parser.readResponse(callback);

        assertEquals(1, response.size());
        assertEquals("cheeseburger", response.getString(0));
    }

    @Test
    public void testParseLiteralWithNonConsumingCallbackReturningNull() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndReturn(0, null);

        ImapResponse response = parser.readResponse(callback);

        assertEquals(1, response.size());
        assertEquals("test", response.getString(0));
        assertTrue(callback.foundLiteralCalled);
        assertAllInputConsumed();
    }

    @Test
    public void readResponse_withPartlyConsumingCallbackReturningNull_shouldThrow() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndReturn(2, null);

        try {
            parser.readResponse(callback);
            fail();
        } catch (AssertionError e) {
            assertEquals("Callback consumed some data but returned no result", e.getMessage());
        }
    }

    @Test
    public void readResponse_withPartlyConsumingCallbackThatThrows_shouldReadAllDataAndThrow() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndThrow(2);

        try {
            parser.readResponse(callback);
            fail();
        } catch (ImapResponseParserException e) {
            assertEquals("readResponse(): Exception in callback method", e.getMessage());
            assertEquals(ImapResponseParserTestException.class, e.getCause().getClass());
        }

        assertAllInputConsumed();
    }

    @Test
    public void readResponse_withCallbackThatThrowsRepeatedly_shouldConsumeAllInputAndThrowFirstException()
            throws Exception {
        ImapResponseParser parser = createParser("* {3}\r\none {3}\r\ntwo\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndThrow(3);

        try {
            parser.readResponse(callback);
            fail();
        } catch (ImapResponseParserException e) {
            assertEquals("readResponse(): Exception in callback method", e.getMessage());
            assertEquals(ImapResponseParserTestException.class, e.getCause().getClass());
            assertEquals(0, ((ImapResponseParserTestException) e.getCause()).instanceNumber);
        }

        assertAllInputConsumed();
    }

    @Test
    public void testParseLiteralWithIncompleteConsumingCallbackReturningString() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        TestImapResponseCallback callback = TestImapResponseCallback.readBytesAndReturn(2, "ninja");

        ImapResponse response = parser.readResponse(callback);

        assertEquals(1, response.size());
        assertEquals("ninja", response.getString(0));
        assertAllInputConsumed();
    }

    @Test
    public void testParseLiteralWithThrowingCallback() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        ImapResponseCallback callback = TestImapResponseCallback.readBytesAndThrow(0);

        try {
            parser.readResponse(callback);
            fail();
        } catch (ImapResponseParserException e) {
            assertEquals("readResponse(): Exception in callback method", e.getMessage());
        }
        
        assertAllInputConsumed();
    }

    @Test(expected = IOException.class)
    public void testParseLiteralWithCallbackThrowingIOException() throws Exception {
        ImapResponseParser parser = createParser("* {4}\r\ntest\r\n");
        ImapResponseCallback callback = new ImapResponseCallback() {
            @Override
            public Object foundLiteral(ImapResponse response, FixedLengthInputStream literal) throws Exception {
                throw new IOException();
            }
        };

        parser.readResponse(callback);
    }

    @Test
    public void testParseQuoted() throws Exception {
        ImapResponseParser parser = createParser("* \"qu\\\"oted\"\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(1, response.size());
        assertEquals("qu\"oted", response.getString(0));
    }

    @Test(expected = IOException.class)
    public void testParseQuotedToEndOfStream() throws Exception {
        ImapResponseParser parser = createParser("* \"abc");

        parser.readResponse();
    }

    @Test(expected = IOException.class)
    public void testParseAtomToEndOfStream() throws Exception {
        ImapResponseParser parser = createParser("* abc");

        parser.readResponse();
    }

    @Test(expected = IOException.class)
    public void testParseUntaggedResponseWithoutSpace() throws Exception {
        ImapResponseParser parser = createParser("*\r\n");

        parser.readResponse();
    }

    @Test
    public void testListResponseContainingFolderNameWithBrackets() throws Exception {
        ImapResponseParser parser = createParser("* LIST (\\HasNoChildren) \".\" [FolderName]\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(4, response.size());
        assertEquals("LIST", response.get(0));
        assertEquals(1, response.getList(1).size());
        assertEquals("\\HasNoChildren", response.getList(1).getString(0));
        assertEquals(".", response.get(2));
        assertEquals("[FolderName]", response.get(3));
    }

    @Test
    public void readResponseShouldReadWholeListResponseLine() throws Exception {
        ImapResponseParser parser = createParser("* LIST (\\HasNoChildren) \".\" [FolderName]\r\n" +
                "TAG OK [List complete]\r\n");
        parser.readResponse();

        ImapResponse responseTwo = parser.readResponse();

        assertEquals("TAG", responseTwo.getTag());
    }

    @Test
    public void readResponse_withListResponseContainingNil() throws Exception {
        ImapResponseParser parser = createParser("* LIST (\\NoInferiors) NIL INBOX\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(4, response.size());
        assertEquals("LIST", response.get(0));
        assertEquals(1, response.getList(1).size());
        assertEquals("\\NoInferiors", response.getList(1).getString(0));
        assertEquals(null, response.get(2));
        assertEquals("INBOX", response.get(3));
    }

    @Test
    public void readResponse_withListAsFirstToken_shouldThrow() throws Exception {
        ImapResponseParser parser = createParser("* [1 2] 3\r\n");

        try {
            parser.readResponse();
            fail("Expected exception");
        } catch (IOException e) {
            assertEquals("Unexpected non-string token: [1, 2]", e.getMessage());
        }
    }

    @Test
    public void testFetchResponse() throws Exception {
        ImapResponseParser parser = createParser("* 1 FETCH (" +
                "UID 23 " +
                "INTERNALDATE \"01-Jul-2015 12:34:56 +0200\" " +
                "RFC822.SIZE 3456 " +
                "BODY[HEADER.FIELDS (date subject from)] \"<headers>\" " +
                "FLAGS (\\Seen))\r\n");

        ImapResponse response = parser.readResponse();

        assertEquals(3, response.size());
        assertEquals("1", response.getString(0));
        assertEquals("FETCH", response.getString(1));
        assertEquals("UID", response.getList(2).getString(0));
        assertEquals(23, response.getList(2).getNumber(1));
        assertEquals("INTERNALDATE", response.getList(2).getString(2));
        assertEquals("01-Jul-2015 12:34:56 +0200", response.getList(2).getString(3));
        assertEquals("RFC822.SIZE", response.getList(2).getString(4));
        assertEquals(3456, response.getList(2).getNumber(5));
        assertEquals("BODY", response.getList(2).getString(6));
        assertEquals(2, response.getList(2).getList(7).size());
        assertEquals("HEADER.FIELDS", response.getList(2).getList(7).getString(0));
        assertEquals(3, response.getList(2).getList(7).getList(1).size());
        assertEquals("date", response.getList(2).getList(7).getList(1).getString(0));
        assertEquals("subject", response.getList(2).getList(7).getList(1).getString(1));
        assertEquals("from", response.getList(2).getList(7).getList(1).getString(2));
        assertEquals("<headers>", response.getList(2).getString(8));
        assertEquals("FLAGS", response.getList(2).getString(9));
        assertEquals(1, response.getList(2).getList(10).size());
        assertEquals("\\Seen", response.getList(2).getList(10).getString(0));
    }

    @Test
    public void readStatusResponse_withNoResponse_shouldThrow() throws Exception {
        ImapResponseParser parser = createParser("1 NO\r\n");

        try {
            parser.readStatusResponse("1", "COMMAND", "[logId]", null);
            fail("Expected exception");
        } catch (NegativeImapResponseException e) {
            assertEquals("Command: COMMAND; response: #1# [NO]", e.getMessage());
        }
    }

    @Test
    public void readStatusResponse_withNoResponseAndAlertText_shouldThrowWithAlertText() throws Exception {
        ImapResponseParser parser = createParser("1 NO [ALERT] Access denied\r\n");

        try {
            parser.readStatusResponse("1", "COMMAND", "[logId]", null);
            fail("Expected exception");
        } catch (NegativeImapResponseException e) {
            assertEquals("Access denied", e.getAlertText());
        }
    }

    private ImapResponseParser createParser(String response) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(response.getBytes());
        peekableInputStream = new PeekableInputStream(byteArrayInputStream);
        return new ImapResponseParser(peekableInputStream);
    }

    private void assertAllInputConsumed() throws IOException {
        assertEquals(0, peekableInputStream.available());
    }


    static class TestImapResponseCallback implements ImapResponseCallback {
        private final int readNumberOfBytes;
        private final Object returnValue;
        private final boolean throwException;
        private int exceptionCount = 0;
        public boolean foundLiteralCalled = false;

        public static TestImapResponseCallback readBytesAndReturn(int readNumberOfBytes, Object returnValue) {
            return new TestImapResponseCallback(readNumberOfBytes, returnValue, false);
        }

        public static TestImapResponseCallback readBytesAndThrow(int readNumberOfBytes) {
            return new TestImapResponseCallback(readNumberOfBytes, null, true);
        }

        private TestImapResponseCallback(int readNumberOfBytes, Object returnValue, boolean throwException) {
            this.readNumberOfBytes = readNumberOfBytes;
            this.returnValue = returnValue;
            this.throwException = throwException;
        }

        @Override
        public Object foundLiteral(ImapResponse response, FixedLengthInputStream literal) throws Exception {
            foundLiteralCalled = true;

            int skipBytes = readNumberOfBytes;
            while (skipBytes > 0) {
                long skippedBytes = literal.skip(skipBytes);
                skipBytes -= skippedBytes;
            }
            
            if (throwException) {
                throw new ImapResponseParserTestException(exceptionCount++);
            }

            return returnValue;
        }
    }

    static class ImapResponseParserTestException extends RuntimeException {
        public final int instanceNumber;

        public ImapResponseParserTestException(int instanceNumber) {
            this.instanceNumber = instanceNumber;
        }
    } 
    
    static class TestUntaggedHandler implements UntaggedHandler {
        public final List<ImapResponse> responses = new ArrayList<ImapResponse>();

        @Override
        public void handleAsyncUntaggedResponse(ImapResponse response) {
            responses.add(response);
        }
    }
}
