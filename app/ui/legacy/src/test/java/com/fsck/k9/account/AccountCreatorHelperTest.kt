import app.k9mail.core.common.mail.Protocols
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.Account.DeletePolicy
import com.fsck.k9.account.AccountCreatorHelper
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
}
