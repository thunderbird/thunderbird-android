package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.Account
import org.junit.Test

class DeletePolicyHelperTest {

    @Test
    fun `getDefaultDeletePolicy with IMAP should return ON_DELETE`() {
        val result = DeletePolicyHelper.getDefaultDeletePolicy(Protocols.IMAP)

        assertThat(result).isEqualTo(Account.DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDefaultDeletePolicy with POP3 should return NEVER`() {
        val result = DeletePolicyHelper.getDefaultDeletePolicy(Protocols.POP3)

        assertThat(result).isEqualTo(Account.DeletePolicy.NEVER)
    }

    @Test
    fun `getDefaultDeletePolicy with demo should return ON_DELETE`() {
        val result = DeletePolicyHelper.getDefaultDeletePolicy("demo")

        assertThat(result).isEqualTo(Account.DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDefaultDeletePolicy with SMTP should fail`() {
        assertFailure {
            DeletePolicyHelper.getDefaultDeletePolicy(Protocols.SMTP)
        }.isInstanceOf<AssertionError>()
    }
}
