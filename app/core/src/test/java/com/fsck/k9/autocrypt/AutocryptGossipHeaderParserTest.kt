package com.fsck.k9.autocrypt

import com.fsck.k9.mail.crlf
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mailstore.MimePartStreamParser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AutocryptGossipHeaderParserTest {
    val GOSSIP_DATA_BOB = Base64.decodeBase64(
        """
        mQGNBFoBt74BDAC8AMsjPY17kxodbfmHah38ZQipY0yfuo97WUBs2jeiFYlQdunPANi5VMgbAX+H
        Mq1XoBRs6qW+WpX8Uj11mu22c57BTUXJRbRr4TnTuuOQmT0egwFDe3x8vHSFmcf9OzG8iKR9ftUE
        +F2ewrzzmm3XY8hy7QeUgBfClZVA6A3rsX4gGawjDo6ZRBbYwckINgGX/vQk6rGs
        """.trimIndent().toByteArray()
    )

    val GOSSIP_HEADER_BOB =
        """
        |addr=bob@autocrypt.example; keydata=
        | mQGNBFoBt74BDAC8AMsjPY17kxodbfmHah38ZQipY0yfuo97WUBs2jeiFYlQdunPANi5VMgbAX+H
        | Mq1XoBRs6qW+WpX8Uj11mu22c57BTUXJRbRr4TnTuuOQmT0egwFDe3x8vHSFmcf9OzG8iKR9ftUE
        | +F2ewrzzmm3XY8hy7QeUgBfClZVA6A3rsX4gGawjDo6ZRBbYwckINgGX/vQk6rGs
        """.trimMargin()

    val GOSSIP_RAW_HEADER_BOB = "Autocrypt-Gossip: $GOSSIP_HEADER_BOB".crlf()

    // Example from Autocrypt 1.0 appendix
    val GOSSIP_PART =
        """
        |Autocrypt-Gossip: $GOSSIP_HEADER_BOB
        |Autocrypt-Gossip: addr=carol@autocrypt.example; keydata=
        | mQGNBFoBt8oBDADGqfZ6PqW05hUEO1dkKm+ixJXnbVriPz2tRkAqT7lTF4KBGitxo4IPv9RPIjJR
        | UMUo89ddyqQfiwKxdFCMDqFDnVRWlDaM+r8sauNJoIFwtTFuvUpkFeCI5gYvneEIIbf1r3Xx1pf5
        | Iy9qsd5eg/4Vvc2AezUv+A6p2DUNHgFMX2FfDus+EPO0wgeWbNaV601aE7UhyugB
        |Content-Type: text/plain
        |
        |Hi Bob and Carol,
        |
        |I wanted to introduce the two of you to each other.
        |
        |I hope you are both doing well!  You can now both "reply all" here,
        |and the thread will remain encrypted.
        |
        |Regards,
        |Alice
        """.trimMargin().crlf()

    private val autocryptGossipHeaderParser = AutocryptGossipHeaderParser.getInstance()

    @Test
    fun parseFromPart() {
        val gossipPart = MimePartStreamParser.parse(null, GOSSIP_PART.byteInputStream())
        val allAutocryptGossipHeaders = autocryptGossipHeaderParser.getAllAutocryptGossipHeaders(gossipPart)

        assertEquals("text/plain", gossipPart.mimeType)
        assertEquals(2, allAutocryptGossipHeaders.size)
        assertEquals("bob@autocrypt.example", allAutocryptGossipHeaders[0].addr)
        assertEquals("carol@autocrypt.example", allAutocryptGossipHeaders[1].addr)
        assertArrayEquals(GOSSIP_DATA_BOB, allAutocryptGossipHeaders[0].keyData)
    }

    @Test
    fun parseString() {
        val gossipHeader = autocryptGossipHeaderParser.parseAutocryptGossipHeader(GOSSIP_HEADER_BOB)

        gossipHeader!!
        assertArrayEquals(GOSSIP_DATA_BOB, gossipHeader.keyData)
        assertEquals(GOSSIP_RAW_HEADER_BOB, gossipHeader.toRawHeaderString())
    }

    @Test
    fun parseHeader_missingKeydata() {
        val gossipHeader = autocryptGossipHeaderParser.parseAutocryptGossipHeader(
            "addr=CDEF"
        )

        assertNull(gossipHeader)
    }

    @Test
    fun parseHeader_unknownCritical() {
        val gossipHeader = autocryptGossipHeaderParser.parseAutocryptGossipHeader(
            "addr=bawb; somecritical=value; keydata=aGk"
        )

        assertNull(gossipHeader)
    }

    @Test
    fun parseHeader_unknownNonCritical() {
        val gossipHeader = autocryptGossipHeaderParser.parseAutocryptGossipHeader(
            "addr=bawb; _somenoncritical=value; keydata=aGk"
        )

        assertNotNull(gossipHeader)
    }

    @Test
    fun parseHeader_brokenBase64() {
        val gossipHeader = autocryptGossipHeaderParser.parseAutocryptGossipHeader(
            "addr=bawb; _somenoncritical=value; keydata=X"
        )

        assertNull(gossipHeader)
    }
}
