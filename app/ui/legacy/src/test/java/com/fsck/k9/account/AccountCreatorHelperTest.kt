import app.k9mail.core.common.mail.Protocols
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.Account.DeletePolicy
import com.fsck.k9.account.AccountCreatorHelper
import com.fsck.k9.mail.ConnectionSecurity
import org.junit.Test

class AccountCreatorHelperTest {
    private val accountCreatorHelper = AccountCreatorHelper()

    @Test
    fun `getDefaultDeletePolicy with IMAP should return ON_DELETE`() {
        val result = accountCreatorHelper.getDefaultDeletePolicy(Protocols.IMAP)

        assertThat(result).isEqualTo(DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDefaultDeletePolicy with POP3 should return NEVER`() {
        val result = accountCreatorHelper.getDefaultDeletePolicy(Protocols.POP3)

        assertThat(result).isEqualTo(DeletePolicy.NEVER)
    }

    @Test
    fun `getDefaultDeletePolicy with SMTP should fail`() {
        assertFailure {
            accountCreatorHelper.getDefaultDeletePolicy(Protocols.SMTP)
        }.isInstanceOf<AssertionError>()
    }

    @Test
    fun `getDefaultPort with NoConnectionSecurity and IMAP should return default port`() {
        val result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.NONE, Protocols.IMAP)

        assertThat(result).isEqualTo(143)
    }

    @Test
    fun `getDefaultPort with StartTls and IMAP should return default port`() {
        val result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.STARTTLS_REQUIRED, Protocols.IMAP)

        assertThat(result).isEqualTo(143)
    }

    @Test
    fun `getDefaultPort with Tls and IMAP should return default Tls port`() {
        val result = accountCreatorHelper.getDefaultPort(ConnectionSecurity.SSL_TLS_REQUIRED, Protocols.IMAP)

        assertThat(result).isEqualTo(993)
    }
}
