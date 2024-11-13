package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import app.k9mail.legacy.account.Account
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.Test

class DefaultDeletePolicyProviderTest {
    private val deletePolicyProvider = DefaultDeletePolicyProvider()

    @Test
    fun `getDeletePolicy with IMAP should return ON_DELETE`() {
        val result = deletePolicyProvider.getDeletePolicy(Protocols.IMAP)

        assertThat(result).isEqualTo(Account.DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDeletePolicy with POP3 should return NEVER`() {
        val result = deletePolicyProvider.getDeletePolicy(Protocols.POP3)

        assertThat(result).isEqualTo(Account.DeletePolicy.NEVER)
    }

    @Test
    fun `getDeletePolicy with demo should return ON_DELETE`() {
        val result = deletePolicyProvider.getDeletePolicy("demo")

        assertThat(result).isEqualTo(Account.DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDeletePolicy with SMTP should fail`() {
        assertFailure {
            deletePolicyProvider.getDeletePolicy(Protocols.SMTP)
        }.isInstanceOf<AssertionError>()
    }
}
