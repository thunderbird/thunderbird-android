package com.fsck.k9.helper

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.AddressHeaderBuilder
import com.fsck.k9.mail.internet.MimeMessage
import java.util.UUID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test

class IdentityHelperTest : RobolectricTest() {
    private val account = createDummyAccount()

    @Test
    fun getRecipientIdentityFromMessage_prefersToOverCc() {
        val message = messageWithRecipients(
            RecipientType.TO to IDENTITY_1_ADDRESS,
            RecipientType.CC to IDENTITY_2_ADDRESS,
            RecipientType.X_ORIGINAL_TO to IDENTITY_3_ADDRESS,
            RecipientType.DELIVERED_TO to IDENTITY_4_ADDRESS,
            RecipientType.X_ENVELOPE_TO to IDENTITY_5_ADDRESS,
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(IDENTITY_1_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_prefersCcOverXOriginalTo() {
        val message = messageWithRecipients(
            RecipientType.TO to "unrelated1@example.org",
            RecipientType.CC to IDENTITY_2_ADDRESS,
            RecipientType.X_ORIGINAL_TO to IDENTITY_3_ADDRESS,
            RecipientType.DELIVERED_TO to IDENTITY_4_ADDRESS,
            RecipientType.X_ENVELOPE_TO to IDENTITY_5_ADDRESS,
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(IDENTITY_2_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_prefersXOriginalToOverDeliveredTo() {
        val message = messageWithRecipients(
            RecipientType.TO to "unrelated1@example.org",
            RecipientType.CC to "unrelated2@example.org",
            RecipientType.X_ORIGINAL_TO to IDENTITY_3_ADDRESS,
            RecipientType.DELIVERED_TO to IDENTITY_4_ADDRESS,
            RecipientType.X_ENVELOPE_TO to IDENTITY_5_ADDRESS,
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(IDENTITY_3_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_prefersDeliveredToOverXEnvelopeTo() {
        val message = messageWithRecipients(
            RecipientType.TO to "unrelated1@example.org",
            RecipientType.CC to "unrelated2@example.org",
            RecipientType.X_ORIGINAL_TO to "unrelated3@example.org",
            RecipientType.DELIVERED_TO to IDENTITY_4_ADDRESS,
            RecipientType.X_ENVELOPE_TO to IDENTITY_5_ADDRESS,
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(IDENTITY_4_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_usesXEnvelopeToWhenPresent() {
        val message = messageWithRecipients(
            RecipientType.TO to "unrelated1@example.org",
            RecipientType.CC to "unrelated2@example.org",
            RecipientType.X_ORIGINAL_TO to "unrelated3@example.org",
            RecipientType.DELIVERED_TO to "unrelated4@example.org",
            RecipientType.X_ENVELOPE_TO to IDENTITY_5_ADDRESS,
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(IDENTITY_5_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_withoutAnyIdentityAddresses_returnsFirstIdentity() {
        val message = messageWithRecipients(
            RecipientType.TO to "unrelated1@example.org",
            RecipientType.CC to "unrelated2@example.org",
            RecipientType.X_ORIGINAL_TO to "unrelated3@example.org",
            RecipientType.DELIVERED_TO to "unrelated4@example.org",
            RecipientType.X_ENVELOPE_TO to "unrelated5@example.org",
        )

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, message)

        assertThat(identity.email).isEqualTo(DEFAULT_ADDRESS)
    }

    @Test
    fun getRecipientIdentityFromMessage_withNoApplicableHeaders_returnsFirstIdentity() {
        val emptyMessage = MimeMessage()

        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, emptyMessage)

        assertThat(identity.email).isEqualTo(DEFAULT_ADDRESS)
    }

    private fun createDummyAccount() = LegacyAccountDto(UUID.randomUUID().toString()).apply {
        replaceIdentities(
            listOf(
                newIdentity("Default", DEFAULT_ADDRESS),
                newIdentity("Identity 1", IDENTITY_1_ADDRESS),
                newIdentity("Identity 2", IDENTITY_2_ADDRESS),
                newIdentity("Identity 3", IDENTITY_3_ADDRESS),
                newIdentity("Identity 4", IDENTITY_4_ADDRESS),
                newIdentity("Identity 5", IDENTITY_5_ADDRESS),
            ),
        )
    }

    private fun newIdentity(name: String, email: String) = Identity(
        name = name,
        email = email,
    )

    private fun messageWithRecipients(vararg recipients: Pair<RecipientType, String>): Message {
        return MimeMessage().apply {
            for ((recipientType, email) in recipients) {
                val headerName = recipientType.toHeaderName()
                addHeader(headerName, AddressHeaderBuilder.createHeaderValue(arrayOf(Address(email))))
            }
        }
    }

    private fun RecipientType.toHeaderName() = when (this) {
        RecipientType.TO -> "To"
        RecipientType.CC -> "Cc"
        RecipientType.BCC -> "Bcc"
        RecipientType.X_ORIGINAL_TO -> "X-Original-To"
        RecipientType.DELIVERED_TO -> "Delivered-To"
        RecipientType.X_ENVELOPE_TO -> "X-Envelope-To"
    }

    companion object {
        const val DEFAULT_ADDRESS = "default@example.org"
        const val IDENTITY_1_ADDRESS = "identity1@example.org"
        const val IDENTITY_2_ADDRESS = "identity2@example.org"
        const val IDENTITY_3_ADDRESS = "identity3@example.org"
        const val IDENTITY_4_ADDRESS = "identity4@example.org"
        const val IDENTITY_5_ADDRESS = "identity5@example.org"
    }
}
