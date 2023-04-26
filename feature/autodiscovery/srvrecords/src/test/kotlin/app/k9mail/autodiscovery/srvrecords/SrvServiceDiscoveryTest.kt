package app.k9mail.autodiscovery.srvrecords

import app.k9mail.autodiscovery.api.DiscoveryResults
import com.fsck.k9.mail.ConnectionSecurity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SrvServiceDiscoveryTest {

    @Test
    fun discover_whenNoMailServices_shouldReturnNoResults() {
        val srvResolver = newMockSrvResolver()

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")

        assertEquals(DiscoveryResults(listOf(), listOf()), result)
    }

    @Test
    fun discover_whenNoSMTP_shouldReturnJustIMAP() {
        val srvResolver = newMockSrvResolver(
            imapServices = listOf(newMailService(port = 143, srvType = SrvType.IMAP)),
            imapsServices = listOf(
                newMailService(port = 993, srvType = SrvType.IMAPS, security = ConnectionSecurity.SSL_TLS_REQUIRED),
            ),
        )

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")

        assertEquals(2, result!!.incoming.size)
        assertEquals(0, result.outgoing.size)
    }

    @Test
    fun discover_whenNoIMAP_shouldReturnJustSMTP() {
        val srvResolver = newMockSrvResolver(
            submissionServices = listOf(
                newMailService(
                    port = 25,
                    srvType = SrvType.SUBMISSION,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                ),
                newMailService(
                    port = 465,
                    srvType = SrvType.SUBMISSIONS,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                ),
            ),
        )

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")

        assertEquals(0, result!!.incoming.size)
        assertEquals(2, result.outgoing.size)
    }

    @Test
    fun discover_withRequiredServices_shouldCorrectlyPrioritize() {
        val srvResolver = newMockSrvResolver(
            submissionServices = listOf(
                newMailService(
                    host = "smtp1.example.com",
                    port = 25,
                    srvType = SrvType.SUBMISSION,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    priority = 0,
                ),
                newMailService(
                    host = "smtp2.example.com",
                    port = 25,
                    srvType = SrvType.SUBMISSION,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    priority = 1,
                ),
            ),
            submissionsServices = listOf(
                newMailService(
                    host = "smtp3.example.com",
                    port = 465,
                    srvType = SrvType.SUBMISSIONS,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    priority = 0,
                ),
                newMailService(
                    host = "smtp4.example.com",
                    port = 465,
                    srvType = SrvType.SUBMISSIONS,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    priority = 1,
                ),
            ),
            imapServices = listOf(
                newMailService(
                    host = "imap1.example.com",
                    port = 143,
                    srvType = SrvType.IMAP,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    priority = 0,
                ),
                newMailService(
                    host = "imap2.example.com",
                    port = 143,
                    srvType = SrvType.IMAP,
                    security = ConnectionSecurity.STARTTLS_REQUIRED,
                    priority = 1,
                ),
            ),
            imapsServices = listOf(
                newMailService(
                    host = "imaps1.example.com",
                    port = 993,
                    srvType = SrvType.IMAPS,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    priority = 0,
                ),
                newMailService(
                    host = "imaps2.example.com",
                    port = 993,
                    srvType = SrvType.IMAPS,
                    security = ConnectionSecurity.SSL_TLS_REQUIRED,
                    priority = 1,
                ),
            ),
        )

        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")

        assertEquals(
            listOf(
                "smtp3.example.com",
                "smtp1.example.com",
                "smtp4.example.com",
                "smtp2.example.com",
            ),
            result?.outgoing?.map { it.host },
        )
        assertEquals(
            listOf(
                "imaps1.example.com",
                "imap1.example.com",
                "imaps2.example.com",
                "imap2.example.com",
            ),
            result?.incoming?.map { it.host },
        )
    }

    private fun newMailService(
        host: String = "example.com",
        priority: Int = 0,
        security: ConnectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
        srvType: SrvType,
        port: Int,
    ): MailService {
        return MailService(srvType, host, port, priority, security)
    }

    private fun newMockSrvResolver(
        host: String = "example.com",
        submissionServices: List<MailService> = listOf(),
        submissionsServices: List<MailService> = listOf(),
        imapServices: List<MailService> = listOf(),
        imapsServices: List<MailService> = listOf(),
    ): MiniDnsSrvResolver {
        return mock {
            on { lookup(host, SrvType.SUBMISSION) } doReturn submissionServices
            on { lookup(host, SrvType.SUBMISSIONS) } doReturn submissionsServices
            on { lookup(host, SrvType.IMAP) } doReturn imapServices
            on { lookup(host, SrvType.IMAPS) } doReturn imapsServices
        }
    }
}
