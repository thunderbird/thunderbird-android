package com.fsck.k9.helper

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mail.Address
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val IDENTITY_ADDRESS = "me@domain.example"
private const val ME_TEXT = "me"

class RealAddressFormatterTest : RobolectricTest() {
    private val contactNameProvider = object : ContactNameProvider {
        override fun getNameForAddress(address: String): String? {
            return when (address) {
                "user1@domain.example" -> "Contact One"
                "spoof@domain.example" -> "contact@important.example"
                else -> null
            }
        }
    }

    private val account = Account("uuid").apply {
        identities += Identity(email = IDENTITY_ADDRESS)
    }

    @Test
    fun `single identity`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"))

        assertThat(displayName).isEqualTo(ME_TEXT)
    }

    @Test
    fun `multiple identities`() {
        val account = Account("uuid").apply {
            identities += Identity(description = "My identity", email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val addressFormatter = createAddressFormatter(account)

        val displayName = addressFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"))

        assertThat(displayName).isEqualTo("My identity")
    }

    @Test
    fun `identity without a description`() {
        val account = Account("uuid").apply {
            identities += Identity(name = "My name", email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val addressFormatter = createAddressFormatter(account)

        val displayName = addressFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"))

        assertThat(displayName).isEqualTo("My name")
    }

    @Test
    fun `identity without a description and name`() {
        val account = Account("uuid").apply {
            identities += Identity(email = IDENTITY_ADDRESS)
            identities += Identity(email = "another.one@domain.example")
        }
        val addressFormatter = createAddressFormatter(account)

        val displayName = addressFormatter.getDisplayName(Address(IDENTITY_ADDRESS, "irrelevant"))

        assertThat(displayName).isEqualTo(IDENTITY_ADDRESS)
    }

    @Test
    fun `email address without display name`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("alice@domain.example"))

        assertThat(displayName).isEqualTo("alice@domain.example")
    }

    @Test
    fun `email address with display name`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("alice@domain.example", "Alice"))

        assertThat(displayName).isEqualTo("Alice")
    }

    @Test
    fun `don't look up contact when showContactNames = false`() {
        val addressFormatter = createAddressFormatter(showContactNames = false)

        val displayName = addressFormatter.getDisplayName(Address("user1@domain.example", "User 1"))

        assertThat(displayName).isEqualTo("User 1")
    }

    @Test
    fun `contact lookup`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("user1@domain.example"))

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `contact lookup despite display name`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("user1@domain.example", "User 1"))

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `colored contact name`() {
        val addressFormatter = createAddressFormatter(contactNameColor = Color.RED)

        val displayName = addressFormatter.getDisplayName(Address("user1@domain.example"))

        assertThat(displayName.toString()).isEqualTo("Contact One")
        assertThat(displayName).isInstanceOf(Spannable::class.java)
        val spans = (displayName as Spannable).getSpans<ForegroundColorSpan>(0, displayName.length)
        assertThat(spans.map { it.foregroundColor }).containsExactly(Color.RED)
    }

    @Test
    fun `email address with display name but not showing correspondent names`() {
        val addressFormatter = createAddressFormatter(showCorrespondentNames = false)

        val displayName = addressFormatter.getDisplayName(Address("alice@domain.example", "Alice"))

        assertThat(displayName).isEqualTo("alice@domain.example")
    }

    @Test
    fun `do not show display name that looks like an email address`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("mallory@domain.example", "potus@whitehouse.gov"))

        assertThat(displayName).isEqualTo("mallory@domain.example")
    }

    @Test
    fun `do show display name that contains an @ preceded by an opening parenthesis`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("gitlab@gitlab.example", "username (@username)"))

        assertThat(displayName).isEqualTo("username (@username)")
    }

    @Test
    fun `do show display name that starts with an @`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("address@domain.example", "@username"))

        assertThat(displayName).isEqualTo("@username")
    }

    @Test
    fun `spoof prevention doesn't apply to contact names`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("spoof@domain.example", "contact@important.example"))

        assertThat(displayName).isEqualTo("contact@important.example")
    }

    @Test
    fun `display name matches me text`() {
        val addressFormatter = createAddressFormatter()

        val displayName = addressFormatter.getDisplayName(Address("someone_named_me@domain.example", "ME"))

        assertThat(displayName).isEqualTo("someone_named_me@domain.example")
    }

    private fun createAddressFormatter(
        account: Account = this.account,
        showCorrespondentNames: Boolean = true,
        showContactNames: Boolean = true,
        contactNameColor: Int? = null
    ): RealAddressFormatter {
        return RealAddressFormatter(
            contactNameProvider = contactNameProvider,
            account = account,
            showCorrespondentNames = showCorrespondentNames,
            showContactNames = showContactNames,
            contactNameColor = contactNameColor,
            meText = ME_TEXT
        )
    }
}
