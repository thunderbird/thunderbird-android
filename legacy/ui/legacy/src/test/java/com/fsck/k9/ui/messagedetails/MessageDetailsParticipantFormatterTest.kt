package com.fsck.k9.ui.messagedetails

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.helper.ContactNameProvider
import com.fsck.k9.mail.Address
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test

private const val IDENTITY_NAME = "Alice"
private const val IDENTITY_ADDRESS = "me@domain.example"
private const val TO_ME_TEXT = "to me"
private const val FROM_ME_TEXT = "from me"

class MessageDetailsParticipantFormatterTest : RobolectricTest() {
    private val contactNameProvider = object : ContactNameProvider {
        override fun getNameForAddress(address: String): String? {
            return when (address) {
                "user1@domain.example" -> "Contact One"
                "spoof@domain.example" -> "contact@important.example"
                else -> null
            }
        }
    }

    private val account = LegacyAccountDto(ACCOUNT_ID_RAW).apply {
        identities += Identity(name = IDENTITY_NAME, email = IDENTITY_ADDRESS)
    }

    private val participantFormatter = createParticipantFormatter()

    @Test
    fun `identity address with single identity as recipient`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address(IDENTITY_ADDRESS, "irrelevant"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo(TO_ME_TEXT)
    }

    @Test
    fun `identity address with single identity as sender`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address(IDENTITY_ADDRESS, "irrelevant"),
            account = account,
            isSender = true,
        )

        assertThat(displayName).isEqualTo(FROM_ME_TEXT)
    }

    @Test
    fun `identity address with multiple identities`() {
        val account = LegacyAccountDto(ACCOUNT_ID_RAW).apply {
            identities += Identity(name = IDENTITY_NAME, email = IDENTITY_ADDRESS)
            identities += Identity(
                name = "Another identity",
                email = "irrelevant@domain.example",
            )
        }

        val displayName = participantFormatter.getDisplayName(
            address = Address(IDENTITY_ADDRESS, "irrelevant"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo(IDENTITY_NAME)
    }

    @Test
    fun `identity without a display name`() {
        val account = LegacyAccountDto(ACCOUNT_ID_RAW).apply {
            identities += Identity(name = null, email = IDENTITY_ADDRESS)
        }

        val displayName = participantFormatter.getDisplayName(
            address = Address(IDENTITY_ADDRESS, "Bob"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo(TO_ME_TEXT)
    }

    @Test
    fun `identity and address without a display name`() {
        val account = LegacyAccountDto(ACCOUNT_ID_RAW).apply {
            identities += Identity(name = null, email = IDENTITY_ADDRESS)
            identities += Identity(
                name = "Another identity",
                email = "irrelevant@domain.example",
            )
        }

        val displayName = participantFormatter.getDisplayName(
            address = Address(IDENTITY_ADDRESS),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo(TO_ME_TEXT)
    }

    @Test
    fun `email address without display name`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address("alice@domain.example"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isNull()
    }

    @Test
    fun `email address with display name`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address("alice@domain.example", "Alice"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo("Alice")
    }

    @Test
    fun `don't look up contact when showContactNames = false`() {
        val participantFormatter = createParticipantFormatter(showContactNames = false)

        val displayName = participantFormatter.getDisplayName(
            address = Address("user1@domain.example", "User 1"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo("User 1")
    }

    @Test
    fun `contact lookup`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address("user1@domain.example"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `contact lookup despite display name`() {
        val displayName = participantFormatter.getDisplayName(
            address = Address("user1@domain.example", "User 1"),
            account = account,
            isSender = false,
        )

        assertThat(displayName).isEqualTo("Contact One")
    }

    @Test
    fun `colored contact name`() {
        val participantFormatter = createParticipantFormatter(contactNameColor = Color.RED)

        val displayName = participantFormatter.getDisplayName(
            address = Address("user1@domain.example"),
            account = account,
            isSender = false,
        )

        assertThat(displayName.toString()).isEqualTo("Contact One")
        assertThat(displayName).isNotNull().isInstanceOf<Spannable>()
        val spans = (displayName as Spannable).getSpans<ForegroundColorSpan>(0, displayName.length)
        assertThat(spans.map { it.foregroundColor }).containsExactly(Color.RED)
    }

    private fun createParticipantFormatter(
        showContactNames: Boolean = true,
        contactNameColor: Int? = null,
    ): MessageDetailsParticipantFormatter {
        return RealMessageDetailsParticipantFormatter(
            contactNameProvider = contactNameProvider,
            showContactNames = showContactNames,
            contactNameColor = contactNameColor,
            toMeText = TO_ME_TEXT,
            fromMeText = FROM_ME_TEXT,
        )
    }
}
