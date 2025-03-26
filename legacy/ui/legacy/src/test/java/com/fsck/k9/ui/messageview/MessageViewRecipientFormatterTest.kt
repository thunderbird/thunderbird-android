package com.fsck.k9.ui.messageview

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import app.k9mail.core.android.testing.RobolectricTest
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.account.LegacyAccount
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address
import org.junit.Test

private const val IDENTITY_ADDRESS = "me@domain.example"
private const val ME_TEXT = "me"

class MessageViewRecipientFormatterTest : RobolectricTest() {
    private val contactNameProvider = object : ContactNameProvider {
        override fun getNameForAddress(address: String): String? {
            return when (address) {
                "user1@domain.example" -> "Contact One"
                "spoof@domain.example" -> "contact@important.example"
                else -> null
            }
        }
    }

    private val account = LegacyAccount("uuid").apply {
        identities += Identity(email = IDENTITY_ADDRESS)
    }

    @Test
    fun `single identity`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"), account)

        assertThat(displayName).isEqualTo(ME_TEXT)
    }

    @Test
    fun `multiple identities`() {
        val account = LegacyAccount("uuid").apply {
            identities += Identity(description = "My identity", email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"), account)

        assertThat(displayName).isEqualTo("My identity")
    }

    @Test
    fun `identity without a description`() {
        val account = LegacyAccount("uuid").apply {
            identities += Identity(name = "My name", email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"), account)

        assertThat(displayName).isEqualTo("My name")
    }

    @Test
    fun `identity without a description and name`() {
        val account = LegacyAccount("uuid").apply {
            identities += Identity(email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"), account)

        assertThat(displayName).isEqualTo(IDENTITY_ADDRESS)
    }

    @Test
    fun `email address without display name`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("alice@domain.example"), account)

        assertThat(displayName).isEqualTo("alice@domain.example")
    }

    @Test
    fun `email address with display name`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("alice@domain.example", "Alice"), account)

        assertThat(displayName).isEqualTo("Alice")
    }

    @Test
    fun `don't look up contact when showContactNames = false`() {
        val recipientFormatter = createRecipientFormatter(showContactNames = false)

        val displayName = recipientFormatter.getDisplayName(Address("user1@domain.example", "User 1"), account)

        assertThat(displayName).isEqualTo("User 1")
    }

    @Test
    fun `contact lookup`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("user1@domain.example"), account)

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `contact lookup despite display name`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("user1@domain.example", "User 1"), account)

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `colored contact name`() {
        val recipientFormatter = createRecipientFormatter(contactNameColor = Color.RED)

        val displayName = recipientFormatter.getDisplayName(Address("user1@domain.example"), account)

        assertThat(displayName.toString()).isEqualTo("Contact One")
        assertThat(displayName).isInstanceOf<Spannable>()
        val spans = (displayName as Spannable).getSpans<ForegroundColorSpan>(0, displayName.length)
        assertThat(spans.map { it.foregroundColor }).containsExactly(Color.RED)
    }

    @Test
    fun `email address with display name but not showing correspondent names`() {
        val recipientFormatter = createRecipientFormatter(showCorrespondentNames = false)

        val displayName = recipientFormatter.getDisplayName(Address("alice@domain.example", "Alice"), account)

        assertThat(displayName).isEqualTo("alice@domain.example")
    }

    @Test
    fun `do not show display name that looks like an email address`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(
            Address("mallory@domain.example", "potus@whitehouse.gov"),
            account,
        )

        assertThat(displayName).isEqualTo("mallory@domain.example")
    }

    @Test
    fun `do show display name that contains an @ preceded by an opening parenthesis`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(
            Address("gitlab@gitlab.example", "username (@username)"),
            account,
        )

        assertThat(displayName).isEqualTo("username (@username)")
    }

    @Test
    fun `do show display name that starts with an @`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("address@domain.example", "@username"), account)

        assertThat(displayName).isEqualTo("@username")
    }

    @Test
    fun `spoof prevention doesn't apply to contact names`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(
            Address("spoof@domain.example", "contact@important.example"),
            account,
        )

        assertThat(displayName).isEqualTo("contact@important.example")
    }

    @Test
    fun `display name matches me text`() {
        val recipientFormatter = createRecipientFormatter()

        val displayName = recipientFormatter.getDisplayName(Address("someone_named_me@domain.example", "ME"), account)

        assertThat(displayName).isEqualTo("someone_named_me@domain.example")
    }

    private fun createRecipientFormatter(
        showCorrespondentNames: Boolean = true,
        showContactNames: Boolean = true,
        contactNameColor: Int? = null,
    ): RealMessageViewRecipientFormatter {
        return RealMessageViewRecipientFormatter(
            contactNameProvider = contactNameProvider,
            showCorrespondentNames = showCorrespondentNames,
            showContactNames = showContactNames,
            contactNameColor = contactNameColor,
            meText = ME_TEXT,
        )
    }
}
