package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.testing.message.buildMessage
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import org.junit.Test

private const val IDENTITY_ADDRESS = "me@domain.example"

class DisplayRecipientsExtractorTest {
    private val account = LegacyAccount(ACCOUNT_ID_RAW).apply {
        identities += Identity(
            email = IDENTITY_ADDRESS,
        )
    }

    private val recipientFormatter = object : MessageViewRecipientFormatter {
        override fun getDisplayName(address: Address, account: LegacyAccount): CharSequence {
            return if (account.isAnIdentity(address)) {
                "me"
            } else {
                when (address.address) {
                    "user1@domain.example" -> "Contact One"
                    "user2@domain.example" -> "Contact Two"
                    "user3@domain.example" -> "Contact Three"
                    "user4@domain.example" -> "Contact Four"
                    else -> address.personal ?: address.address
                }
            }
        }
    }

    private val displayRecipientsExtractor = DisplayRecipientsExtractor(
        recipientFormatter,
        maxNumberOfDisplayRecipients = 5,
    )

    @Test
    fun `single recipient is identity address`() {
        val message = buildMessage {
            header("To", "Test User <$IDENTITY_ADDRESS>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("me"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient is identity address with different local part case`() {
        val message = buildMessage {
            header("To", "Test User <ME@domain.example>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("me"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient is identity address with different domain case`() {
        val message = buildMessage {
            header("To", "Test User <me@DOMAIN.EXAMPLE>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("me"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient is identity address with different local part and domain case`() {
        val message = buildMessage {
            header("To", "Test User <ME@DOMAIN.EXAMPLE>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("me"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient is a contact`() {
        val message = buildMessage {
            header("To", "User 1 <user1@domain.example>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("Contact One"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient is not a contact`() {
        val message = buildMessage {
            header("To", "Alice <alice@domain.example>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("Alice"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `single recipient without name and not a contact`() {
        val message = buildMessage {
            header("To", "alice@domain.example")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(recipientNames = listOf("alice@domain.example"), numberOfRecipients = 1),
        )
    }

    @Test
    fun `three unknown recipients`() {
        val message = buildMessage {
            header("To", "Unknown 1 <unknown1@domain.example>, Unknown 2 <unknown2@domain.example>")
            header("Cc", "Unknown 3 <unknown3@domain.example>")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(
                recipientNames = listOf("Unknown 1", "Unknown 2", "Unknown 3"),
                numberOfRecipients = 3,
            ),
        )
    }

    @Test
    fun `recipients spread across To and Cc header`() {
        val message = buildMessage {
            header("To", "user1@domain.example, Alice <alice@domain.example>, $IDENTITY_ADDRESS")
            header("Cc", "user2@domain.example, User 4 <user4@domain.example>, someone.else@domain.example")
            header("Bcc", "hidden@domain.example")
        }

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients).isEqualTo(
            DisplayRecipients(
                recipientNames = listOf("me", "Contact One", "Alice", "Contact Two", "Contact Four"),
                numberOfRecipients = 7,
            ),
        )
    }

    @Test
    fun `100 recipients, RecipientFormatter_getDisplayName should only be called maxNumberOfDisplayRecipients times`() {
        val recipients = (1..100).joinToString(separator = ", ") { "unknown$it@domain.example" }
        val message = buildMessage {
            header("To", recipients)
        }
        var numberOfTimesCalled = 0
        val recipientFormatter = object : MessageViewRecipientFormatter {
            override fun getDisplayName(address: Address, account: LegacyAccount): CharSequence {
                numberOfTimesCalled++
                return address.address
            }
        }
        val displayRecipientsExtractor = DisplayRecipientsExtractor(
            recipientFormatter,
            maxNumberOfDisplayRecipients = 5,
        )

        val displayRecipients = displayRecipientsExtractor.extractDisplayRecipients(message, account)

        assertThat(displayRecipients.numberOfRecipients).isEqualTo(100)
        assertThat(numberOfTimesCalled).isEqualTo(5)
    }
}
