package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.RobolectricTest
import com.fsck.k9.mail.ConnectionSecurity
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert
import org.junit.Test

class SrvServiceDiscoveryTest : RobolectricTest() {

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
        imapServices: List<MailService> = listOf(),
        imapsServices: List<MailService> = listOf()
    ): SrvResolver {
        return mock {
            on { lookup("example.com", SrvType.SUBMISSION) } doReturn submissionServices
            on { lookup("example.com", SrvType.IMAP) } doReturn imapServices
            on { lookup("example.com", SrvType.IMAPS) } doReturn imapsServices
        }
    }

    @Test
    fun discover_whenNoMailServices_shouldReturnNull() {
        val srvResolver = newMockSrvResolver()
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")
        Assert.assertNull(result)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSION)
        verify(srvResolver).lookup("example.com", SrvType.IMAP)
        verify(srvResolver).lookup("example.com", SrvType.IMAPS)
    }

    @Test
    fun discover_whenNoSMTP_shouldReturnNull() {
        val srvResolver = newMockSrvResolver(imapServices = listOf(
            newMailService(port = 143, srvType = SrvType.IMAP)
        ), imapsServices = listOf(
            newMailService(port = 993, srvType = SrvType.IMAPS, security = ConnectionSecurity.SSL_TLS_REQUIRED)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")
        Assert.assertNull(result)
    }

    @Test
    fun discover_whenNoIMAP_shouldReturnNull() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(port = 25, srvType = SrvType.SUBMISSION, security = ConnectionSecurity.NONE)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")
        Assert.assertNull(result)
    }

    @Test
    fun discover_withRequiredServices_shouldCorrectlyPrioritize() {
        val srvResolver = newMockSrvResolver(submissionServices = listOf(
            newMailService(
                host = "smtp1.example.com", port = 25, srvType = SrvType.SUBMISSION,
                security = ConnectionSecurity.NONE, priority = 0),
            newMailService(
                host = "smtp2.example.com", port = 465, srvType = SrvType.SUBMISSION,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 0),
            newMailService(
                host = "smtp3.example.com", port = 465, srvType = SrvType.SUBMISSION,
                security = ConnectionSecurity.STARTTLS_REQUIRED, priority = 1)
        ), imapServices = listOf(
            newMailService(
                host = "imap1.example.com", port = 143, srvType = SrvType.IMAP,
                security = ConnectionSecurity.NONE, priority = 0),
            newMailService(
                host = "imap2.example.com", port = 143, srvType = SrvType.IMAP,
                security = ConnectionSecurity.NONE, priority = 1)
        ), imapsServices = listOf(
            newMailService(
                host = "imaps1.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 0),
            newMailService(
                host = "imaps2.example.com", port = 993, srvType = SrvType.IMAPS,
                security = ConnectionSecurity.SSL_TLS_REQUIRED, priority = 1)
        ))
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")
        Assert.assertEquals("smtp2.example.com", result?.outgoing?.host)
        Assert.assertEquals("imaps1.example.com", result?.incoming?.host)
    }
}
