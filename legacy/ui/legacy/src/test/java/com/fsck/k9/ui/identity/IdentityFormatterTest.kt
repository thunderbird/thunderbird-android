package com.fsck.k9.ui.identity

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.android.account.Identity
import org.junit.Test

private const val IDENTITY_NAME = "Identity Name"
private const val SENDER_NAME = "Display Name"
private const val EMAIL = "test@domain.example"

class IdentityFormatterTest {
    private val identityFormatter = IdentityFormatter()

    @Test
    fun `getDisplayName() with identity name`() {
        val identity = Identity(
            description = IDENTITY_NAME,
            name = "irrelevant",
            email = EMAIL,
        )

        val displayName = identityFormatter.getDisplayName(identity)

        assertThat(displayName).isEqualTo(IDENTITY_NAME)
    }

    @Test
    fun `getDisplayName() without identity name, but sender name`() {
        val identity =
            Identity(description = null, name = SENDER_NAME, email = EMAIL)

        val displayName = identityFormatter.getDisplayName(identity)

        assertThat(displayName).isEqualTo("$SENDER_NAME <$EMAIL>")
    }

    @Test
    fun `getDisplayName() without identity name and sender name`() {
        val identity = Identity(description = null, name = null, email = EMAIL)

        val displayName = identityFormatter.getDisplayName(identity)

        assertThat(displayName).isEqualTo(EMAIL)
    }

    @Test
    fun `getEmailDisplayName() with sender name`() {
        val identity = Identity(name = SENDER_NAME, email = EMAIL)

        val displayName = identityFormatter.getEmailDisplayName(identity)

        assertThat(displayName).isEqualTo("$SENDER_NAME <$EMAIL>")
    }

    @Test
    fun `getEmailDisplayName() without sender name`() {
        val identity = Identity(name = null, email = EMAIL)

        val displayName = identityFormatter.getEmailDisplayName(identity)

        assertThat(displayName).isEqualTo(EMAIL)
    }

    @Test
    fun `getEmailDisplayName() should ignore identity name`() {
        val identity =
            Identity(description = IDENTITY_NAME, name = null, email = EMAIL)

        val displayName = identityFormatter.getEmailDisplayName(identity)

        assertThat(displayName).isEqualTo(EMAIL)
    }
}
