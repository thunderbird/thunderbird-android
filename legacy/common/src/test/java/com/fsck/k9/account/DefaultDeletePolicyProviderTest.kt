package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.thunderbird.core.android.account.DeletePolicy
import org.junit.Test

class DefaultDeletePolicyProviderTest {
    private val deletePolicyProvider = DefaultDeletePolicyProvider()

    @Test
    fun `getDeletePolicy with IMAP should return ON_DELETE`() {
        val result = deletePolicyProvider.getDeletePolicy(Protocols.IMAP)

        assertThat(result).isEqualTo(DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDeletePolicy with POP3 should return NEVER`() {
        val result = deletePolicyProvider.getDeletePolicy(Protocols.POP3)

        assertThat(result).isEqualTo(DeletePolicy.NEVER)
    }

    @Test
    fun `getDeletePolicy with demo should return ON_DELETE`() {
        val result = deletePolicyProvider.getDeletePolicy("demo")

        assertThat(result).isEqualTo(DeletePolicy.ON_DELETE)
    }

    @Test
    fun `getDeletePolicy with SMTP should fail`() {
        assertFailure {
            deletePolicyProvider.getDeletePolicy(Protocols.SMTP)
        }.isInstanceOf<AssertionError>()
    }
}
