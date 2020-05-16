package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.RobolectricTest
import com.fsck.k9.autodiscovery.DiscoveryResults
import com.fsck.k9.autodiscovery.DiscoveryTarget
import com.fsck.k9.mail.ConnectionSecurity
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

class SrvServiceDiscoveryTest : RobolectricTest() {

    @Test
    fun discover_whenNoMailServices_shouldReturnNull() {
        val srvResolver = newMockSrvResolver()

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING_AND_OUTGOING)

        assertEquals(DiscoveryResults(listOf(), listOf()), result)
    }

    @Ignore
    @Test
    fun discover_whenNoSMTP_shouldReturnNull() {
        val srvResolver = newMockSrvResolver(
            imapServices = listOf(newMailService(port = 143, srvType = SrvType.IMAP)),
            imapsServices = listOf(newMailService(port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED))
        )
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING_AND_OUTGOING)

        assertNull(result)
    }

    @Ignore
    @Test
    fun discover_whenNoIMAP_shouldReturnNull() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(port = 25, srvType = SrvType.SUBMISSION, security = ConnectionSecurity.STARTTLS_REQUIRED),
            newMailService(port = 465, srvType = SrvType.SUBMISSIONS, security = ConnectionSecurity.SSL_TLS_REQUIRED)
        ))

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING_AND_OUTGOING)

        assertNull(result)
    }

    @Test
    fun discover_withRequiredServices_shouldCorrectlyPrioritize() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "smtp1.example.com", port = 25, srvType = SrvType.SUBMISSION,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 0),
            newMailService(
                host = "smtp2.example.com", port = 25, srvType = SrvType.SUBMISSION,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 1)
        ), submissionsServices = listOf(
            newMailService(
                host = "smtp3.example.com", port = 465, srvType = SrvType.SUBMISSIONS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0),
            newMailService(
                host = "smtp4.example.com", port = 465, srvType = SrvType.SUBMISSIONS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 1)
        ), imapServices = listOf(
            newMailService(
                host = "imap1.example.com", port = 143, srvType = SrvType.IMAP,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 0),
            newMailService(
                host = "imap2.example.com", port = 143, srvType = SrvType.IMAP,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 1)
        ), imapsServices = listOf(
            newMailService(
                host = "imaps1.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0),
            newMailService(
                host = "imaps2.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 1)
        ))

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING_AND_OUTGOING)

        assertEquals("smtp3.example.com", result?.outgoing?.first()?.host)
        assertEquals("imaps1.example.com", result?.incoming?.first()?.host)
    }

    @Ignore
    @Test
    fun discover_whenOnlyOutgoingTrue_shouldOnlyFetchOutgoing() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "smtp.example.com", port = 465, srvType = SrvType.SUBMISSIONS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.OUTGOING)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSIONS)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSION)
        verifyNoMoreInteractions(srvResolver)
        assertEquals("smtp.example.com", result!!.outgoing.first().host)
        assertNull(result.incoming)
    }

    @Ignore
    @Test
    fun discover_whenOnlyIncomingTrue_shouldOnlyFetchIncoming() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "imaps.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING)
        verify(srvResolver).lookup("example.com", SrvType.IMAPS)
        verify(srvResolver).lookup("example.com", SrvType.IMAP)
        verifyNoMoreInteractions(srvResolver)
        assertEquals("imaps.example.com", result!!.incoming?.first()?.host)
        assertNull(result.outgoing)
    }

    @Ignore
    @Test
    fun discover_whenOnlyOutgoingTrue_shouldOnlyFetchOutgoing() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "smtp.example.com", port = 465, srvType = SrvType.SUBMISSIONS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.OUTGOING)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSIONS)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSION)
        verifyNoMoreInteractions(srvResolver)
        assertEquals("smtp.example.com", result!!.outgoing.host)
        assertNull(result.incoming)
    }

    @Ignore
    @Test
    fun discover_whenOnlyIncomingTrue_shouldOnlyFetchIncoming() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "imaps.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com", DiscoveryTarget.INCOMING)
        verify(srvResolver).lookup("example.com", SrvType.IMAPS)
        verify(srvResolver).lookup("example.com", SrvType.IMAP)
        verifyNoMoreInteractions(srvResolver)
        assertEquals("imaps.example.com", result!!.incoming.host)
        assertNull(result.outgoing)
    }

    private fun newMailService(
        host: String = "example.com",
        priority: Int = 0,
        security: ConnectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
        srvType: SrvType,
        port: Int
    ): MailService {
        return MailService(srvType, host, port, priority, security)
    }

    private fun newMockSrvResolver(
        host: String = "example.com",
        submissionServices: List<MailService> = listOf(),
        submissionsServices: List<MailService> = listOf(),
        imapServices: List<MailService> = listOf(),
        imapsServices: List<MailService> = listOf()
    ): MiniDnsSrvResolver {
        return mock {
            on { lookup(host, SrvType.SUBMISSION) } doReturn submissionServices
            on { lookup(host, SrvType.SUBMISSIONS) } doReturn submissionsServices
            on { lookup(host, SrvType.IMAP) } doReturn imapServices
            on { lookup(host, SrvType.IMAPS) } doReturn imapsServices
        }
    }
}
