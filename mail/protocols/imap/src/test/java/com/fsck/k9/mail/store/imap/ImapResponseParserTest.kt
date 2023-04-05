package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.filter.FixedLengthInputStream
import com.fsck.k9.mail.filter.PeekableInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert
import org.junit.Test

class ImapResponseParserTest {
    private var peekableInputStream: PeekableInputStream? = null
    @Test
    @Throws(IOException::class)
    fun testSimpleOkResponse() {
        val parser = createParser("* OK\r\n")
        val response = parser.readResponse()
        Assert.assertNotNull(response)
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("OK", response[0])
    }

    @Test
    @Throws(IOException::class)
    fun testOkResponseWithText() {
        val parser = createParser("* OK Some text here\r\n")
        val response = parser.readResponse()
        Assert.assertNotNull(response)
        Assert.assertEquals(2, response.size.toLong())
        Assert.assertEquals("OK", response[0])
        Assert.assertEquals("Some text here", response[1])
    }

    @Test
    @Throws(IOException::class)
    fun testOkResponseWithRespTextCode() {
        val parser = createParser("* OK [UIDVALIDITY 3857529045]\r\n")
        val response = parser.readResponse()
        Assert.assertNotNull(response)
        Assert.assertEquals(2, response.size.toLong())
        Assert.assertEquals("OK", response[0])
        Assert.assertTrue(response[1] is ImapList)
        val respTextCode = response[1] as ImapList
        Assert.assertEquals(2, respTextCode.size.toLong())
        Assert.assertEquals("UIDVALIDITY", respTextCode[0])
        Assert.assertEquals("3857529045", respTextCode[1])
    }

    @Test
    @Throws(IOException::class)
    fun testOkResponseWithRespTextCodeAndText() {
        val parser = createParser("* OK [token1 token2] {x} test [...]\r\n")
        val response = parser.readResponse()
        Assert.assertNotNull(response)
        Assert.assertEquals(3, response.size.toLong())
        Assert.assertEquals("OK", response[0])
        Assert.assertTrue(response[1] is ImapList)
        Assert.assertEquals("{x} test [...]", response[2])
        val respTextCode = response[1] as ImapList
        Assert.assertEquals(2, respTextCode.size.toLong())
        Assert.assertEquals("token1", respTextCode[0])
        Assert.assertEquals("token2", respTextCode[1])
    }

    @Test
    @Throws(Exception::class)
    fun testReadStatusResponseWithOKResponse() {
        val parser = createParser(
            "* COMMAND BAR\tBAZ\r\n" +
                "TAG OK COMMAND completed\r\n"
        )
        val responses = parser.readStatusResponse("TAG", null, null, null)
        Assert.assertEquals(2, responses.size.toLong())
        Assert.assertEquals(mutableListOf("COMMAND", "BAR", "BAZ"), responses[0])
        Assert.assertEquals(mutableListOf("OK", "COMMAND completed"), responses[1])
    }

    @Test
    @Throws(Exception::class)
    fun testReadStatusResponseUntaggedHandlerGetsUntaggedOnly() {
        val parser = createParser(
            """
                 * UNTAGGED
                 A2 OK COMMAND completed
                 
                 """.trimIndent()
        )
        val untaggedHandler = TestUntaggedHandler()
        parser.readStatusResponse("A2", null, null, untaggedHandler)
        Assert.assertEquals(1, untaggedHandler.responses.size.toLong())
        Assert.assertEquals(mutableListOf("UNTAGGED"), untaggedHandler.responses[0])
    }

    @Test
    @Throws(Exception::class)
    fun testReadStatusResponseSkippingWrongTag() {
        val parser = createParser(
            """
    * UNTAGGED
    * 0 EXPUNGE
    * 42 EXISTS
    A1 COMMAND BAR BAZ
    A2 OK COMMAND completed
    
    """.trimIndent()
        )
        val untaggedHandler = TestUntaggedHandler()
        val responses = parser.readStatusResponse("A2", null, null, untaggedHandler)
        Assert.assertEquals(3, responses.size.toLong())
        Assert.assertEquals(mutableListOf("0", "EXPUNGE"), responses[0])
        Assert.assertEquals(mutableListOf("42", "EXISTS"), responses[1])
        Assert.assertEquals(mutableListOf("OK", "COMMAND completed"), responses[2])
        Assert.assertEquals(mutableListOf("UNTAGGED"), untaggedHandler.responses[0])
        Assert.assertEquals(responses[0], untaggedHandler.responses[1])
        Assert.assertEquals(responses[1], untaggedHandler.responses[2])
    }

    @Test
    @Throws(Exception::class)
    fun testReadStatusResponseUntaggedHandlerStillCalledOnNegativeReply() {
        val parser = createParser(
            """
                 + text
                 A2 NO Bad response
                 
                 """.trimIndent()
        )
        val untaggedHandler = TestUntaggedHandler()
        try {
            val responses = parser.readStatusResponse("A2", null, null, untaggedHandler)
        } catch (e: NegativeImapResponseException) {
        }
        Assert.assertEquals(1, untaggedHandler.responses.size.toLong())
        Assert.assertEquals(mutableListOf("text"), untaggedHandler.responses[0])
    }

    @Test(expected = NegativeImapResponseException::class)
    @Throws(Exception::class)
    fun testReadStatusResponseWithErrorResponse() {
        val parser = createParser("* COMMAND BAR BAZ\r\nTAG ERROR COMMAND errored\r\n")
        parser.readStatusResponse("TAG", null, null, null)
    }

    @Test
    @Throws(Exception::class)
    fun testRespTextCodeWithList() {
        val parser = createParser(
            """
    * OK [PERMANENTFLAGS (\Answered \Flagged \Deleted \Seen \Draft NonJunk ${"$"}MDNSent \*)] Flags permitted.
    
    """.trimIndent()
        )
        val response = parser.readResponse()
        Assert.assertEquals(3, response.size.toLong())
        Assert.assertTrue(response[1] is ImapList)
        Assert.assertEquals(2, response.getList(1).size.toLong())
        Assert.assertEquals("PERMANENTFLAGS", response.getList(1).getString(0))
        Assert.assertTrue(response.getList(1)[1] is ImapList)
        Assert.assertEquals("\\Answered", response.getList(1).getList(1).getString(0))
        Assert.assertEquals("\\Flagged", response.getList(1).getList(1).getString(1))
        Assert.assertEquals("\\Deleted", response.getList(1).getList(1).getString(2))
        Assert.assertEquals("\\Seen", response.getList(1).getList(1).getString(3))
        Assert.assertEquals("\\Draft", response.getList(1).getList(1).getString(4))
        Assert.assertEquals("NonJunk", response.getList(1).getList(1).getString(5))
        Assert.assertEquals("\$MDNSent", response.getList(1).getList(1).getString(6))
        Assert.assertEquals("\\*", response.getList(1).getList(1).getString(7))
    }

    @Test
    @Throws(Exception::class)
    fun testExistsResponse() {
        val parser = createParser("* 23 EXISTS\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(2, response.size.toLong())
        Assert.assertEquals(23, response.getNumber(0).toLong())
        Assert.assertEquals("EXISTS", response.getString(1))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class)
    fun testReadStringUntilEndOfStream() {
        val parser = createParser("* OK Some text ")
        parser.readResponse()
    }

    @Test
    @Throws(Exception::class)
    fun testCommandContinuation() {
        val parser = createParser("+ Ready for additional command text\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("Ready for additional command text", response.getString(0))
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteral() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("test", response.getString(0))
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteralWithEmptyString() {
        val parser = createParser("* {0}\r\n\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("", response.getString(0))
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testParseLiteralToEndOfStream() {
        val parser = createParser("* {4}\r\nabc")
        parser.readResponse()
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteralWithConsumingCallbackReturningNull() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = TestImapResponseCallback.readBytesAndReturn(4, "cheeseburger")
        val response = parser.readResponse(callback)
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("cheeseburger", response.getString(0))
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteralWithNonConsumingCallbackReturningNull() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = TestImapResponseCallback.readBytesAndReturn(0, null)
        val response = parser.readResponse(callback)
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("test", response.getString(0))
        Assert.assertTrue(callback.foundLiteralCalled)
        assertAllInputConsumed()
    }

    @Test
    @Throws(Exception::class)
    fun readResponse_withPartlyConsumingCallbackReturningNull_shouldThrow() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = TestImapResponseCallback.readBytesAndReturn(2, null)
        try {
            parser.readResponse(callback)
            Assert.fail()
        } catch (e: AssertionError) {
            Assert.assertEquals("Callback consumed some data but returned no result", e.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun readResponse_withPartlyConsumingCallbackThatThrows_shouldReadAllDataAndThrow() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = TestImapResponseCallback.readBytesAndThrow(2)
        try {
            parser.readResponse(callback)
            Assert.fail()
        } catch (e: ImapResponseParserException) {
            Assert.assertEquals("readResponse(): Exception in callback method", e.message)
            Assert.assertEquals(ImapResponseParserTestException::class.java, e.cause!!.javaClass)
        }
        assertAllInputConsumed()
    }

    @Test
    @Throws(Exception::class)
    fun readResponse_withCallbackThatThrowsRepeatedly_shouldConsumeAllInputAndThrowFirstException() {
        val parser = createParser("* {3}\r\none {3}\r\ntwo\r\n")
        val callback = TestImapResponseCallback.readBytesAndThrow(3)
        try {
            parser.readResponse(callback)
            Assert.fail()
        } catch (e: ImapResponseParserException) {
            Assert.assertEquals("readResponse(): Exception in callback method", e.message)
            Assert.assertEquals(ImapResponseParserTestException::class.java, e.cause!!.javaClass)
            Assert.assertEquals(0, (e.cause as ImapResponseParserTestException?)!!.instanceNumber.toLong())
        }
        assertAllInputConsumed()
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteralWithIncompleteConsumingCallbackReturningString() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = TestImapResponseCallback.readBytesAndReturn(2, "ninja")
        val response = parser.readResponse(callback)
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("ninja", response.getString(0))
        assertAllInputConsumed()
    }

    @Test
    @Throws(Exception::class)
    fun testParseLiteralWithThrowingCallback() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback: ImapResponseCallback = TestImapResponseCallback.readBytesAndThrow(0)
        try {
            parser.readResponse(callback)
            Assert.fail()
        } catch (e: ImapResponseParserException) {
            Assert.assertEquals("readResponse(): Exception in callback method", e.message)
        }
        assertAllInputConsumed()
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testParseLiteralWithCallbackThrowingIOException() {
        val parser = createParser("* {4}\r\ntest\r\n")
        val callback = ImapResponseCallback { response, literal -> throw IOException() }
        parser.readResponse(callback)
    }

    @Test
    @Throws(Exception::class)
    fun testParseQuoted() {
        val parser = createParser("* \"qu\\\"oted\"\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("qu\"oted", response.getString(0))
    }

    @Test
    @Throws(Exception::class)
    fun utf8InQuotedString() {
        val parser = createParser("* \"quöted\"\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(1, response.size.toLong())
        Assert.assertEquals("quöted", response.getString(0))
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testParseQuotedToEndOfStream() {
        val parser = createParser("* \"abc")
        parser.readResponse()
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testParseAtomToEndOfStream() {
        val parser = createParser("* abc")
        parser.readResponse()
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testParseUntaggedResponseWithoutSpace() {
        val parser = createParser("*\r\n")
        parser.readResponse()
    }

    @Test
    @Throws(Exception::class)
    fun testListResponseContainingFolderNameWithBrackets() {
        val parser = createParser("* LIST (\\HasNoChildren) \".\" [FolderName]\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(4, response.size.toLong())
        Assert.assertEquals("LIST", response[0])
        Assert.assertEquals(1, response.getList(1).size.toLong())
        Assert.assertEquals("\\HasNoChildren", response.getList(1).getString(0))
        Assert.assertEquals(".", response[2])
        Assert.assertEquals("[FolderName]", response[3])
    }

    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testListResponseContainingFolderNameContainingBracketsThrowsException() {
        val parser = createParser(
            "* LIST (\\NoInferiors) \"/\" Root/Folder/Subfolder()\r\n"
        )
        parser.readResponse()
    }

    @Test
    @Throws(Exception::class)
    fun readResponseShouldReadWholeListResponseLine() {
        val parser = createParser(
            """* LIST (\HasNoChildren) "." [FolderName]
TAG OK [List complete]
"""
        )
        parser.readResponse()
        val responseTwo = parser.readResponse()
        Assert.assertEquals("TAG", responseTwo.tag)
    }

    @Test
    @Throws(Exception::class)
    fun readResponse_withListResponseContainingNil() {
        val parser = createParser("* LIST (\\NoInferiors) NIL INBOX\r\n")
        val response = parser.readResponse()
        Assert.assertEquals(4, response.size.toLong())
        Assert.assertEquals("LIST", response[0])
        Assert.assertEquals(1, response.getList(1).size.toLong())
        Assert.assertEquals("\\NoInferiors", response.getList(1).getString(0))
        Assert.assertEquals(null, response[2])
        Assert.assertEquals("INBOX", response[3])
    }

    @Test
    @Throws(Exception::class)
    fun readResponse_withListAsFirstToken_shouldThrow() {
        val parser = createParser("* [1 2] 3\r\n")
        try {
            parser.readResponse()
            Assert.fail("Expected exception")
        } catch (e: IOException) {
            Assert.assertEquals("Unexpected non-string token: ImapList - [1, 2]", e.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFetchResponse() {
        val parser = createParser(
            """* 1 FETCH (UID 23 INTERNALDATE "01-Jul-2015 12:34:56 +0200" RFC822.SIZE 3456 BODY[HEADER.FIELDS (date subject from)] "<headers>" FLAGS (\Seen))
"""
        )
        val response = parser.readResponse()
        Assert.assertEquals(3, response.size.toLong())
        Assert.assertEquals("1", response.getString(0))
        Assert.assertEquals("FETCH", response.getString(1))
        Assert.assertEquals("UID", response.getList(2).getString(0))
        Assert.assertEquals(23, response.getList(2).getNumber(1).toLong())
        Assert.assertEquals("INTERNALDATE", response.getList(2).getString(2))
        Assert.assertEquals("01-Jul-2015 12:34:56 +0200", response.getList(2).getString(3))
        Assert.assertEquals("RFC822.SIZE", response.getList(2).getString(4))
        Assert.assertEquals(3456, response.getList(2).getNumber(5).toLong())
        Assert.assertEquals("BODY", response.getList(2).getString(6))
        Assert.assertEquals(2, response.getList(2).getList(7).size.toLong())
        Assert.assertEquals("HEADER.FIELDS", response.getList(2).getList(7).getString(0))
        Assert.assertEquals(3, response.getList(2).getList(7).getList(1).size.toLong())
        Assert.assertEquals("date", response.getList(2).getList(7).getList(1).getString(0))
        Assert.assertEquals("subject", response.getList(2).getList(7).getList(1).getString(1))
        Assert.assertEquals("from", response.getList(2).getList(7).getList(1).getString(2))
        Assert.assertEquals("<headers>", response.getList(2).getString(8))
        Assert.assertEquals("FLAGS", response.getList(2).getString(9))
        Assert.assertEquals(1, response.getList(2).getList(10).size.toLong())
        Assert.assertEquals("\\Seen", response.getList(2).getList(10).getString(0))
    }

    @Test
    @Throws(Exception::class)
    fun readStatusResponse_withNoResponse_shouldThrow() {
        val parser = createParser("1 NO\r\n")
        try {
            parser.readStatusResponse("1", "COMMAND", "[logId]", null)
            Assert.fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            Assert.assertEquals("Command: COMMAND; response: #1# [NO]", e.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun readStatusResponse_withNoResponseAndAlertText_shouldThrowWithAlertText() {
        val parser = createParser("1 NO [ALERT] Access denied\r\n")
        try {
            parser.readStatusResponse("1", "COMMAND", "[logId]", null)
            Assert.fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            Assert.assertEquals("Access denied", e.alertText)
        }
    }

    private fun createParser(response: String): ImapResponseParser {
        val byteArrayInputStream = ByteArrayInputStream(response.toByteArray(UTF_8))
        peekableInputStream = PeekableInputStream(byteArrayInputStream)
        return ImapResponseParser(peekableInputStream)
    }

    @Throws(IOException::class)
    private fun assertAllInputConsumed() {
        Assert.assertEquals(0, peekableInputStream!!.available().toLong())
    }

    internal class TestImapResponseCallback private constructor(
        private val readNumberOfBytes: Int,
        private val returnValue: Any,
        private val throwException: Boolean
    ) : ImapResponseCallback {
        private var exceptionCount = 0
        var foundLiteralCalled = false
        @Throws(Exception::class)
        override fun foundLiteral(response: ImapResponse, literal: FixedLengthInputStream): Any {
            foundLiteralCalled = true
            var skipBytes = readNumberOfBytes
            while (skipBytes > 0) {
                val skippedBytes = literal.skip(skipBytes.toLong())
                skipBytes -= skippedBytes.toInt()
            }
            if (throwException) {
                throw ImapResponseParserTestException(exceptionCount++)
            }
            return returnValue
        }

        companion object {
            fun readBytesAndReturn(readNumberOfBytes: Int, returnValue: Any?): TestImapResponseCallback {
                return TestImapResponseCallback(readNumberOfBytes, returnValue!!, false)
            }

            fun readBytesAndThrow(readNumberOfBytes: Int): TestImapResponseCallback {
                return TestImapResponseCallback(readNumberOfBytes, null, true)
            }
        }
    }

    internal class ImapResponseParserTestException(val instanceNumber: Int) : RuntimeException()
    internal class TestUntaggedHandler : UntaggedHandler {
        val responses: MutableList<ImapResponse> = ArrayList()
        override fun handleAsyncUntaggedResponse(response: ImapResponse) {
            responses.add(response)
        }
    }
}
