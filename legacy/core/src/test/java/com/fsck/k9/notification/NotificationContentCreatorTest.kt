package com.fsck.k9.notification

import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.display.visualSettings.message.list.DisplayMessageListSettings
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val FOLDER_ID = 23L
private const val UID = "42"
private const val PREVIEW = "Message preview text"
private const val SUBJECT = "Message subject"
private const val SENDER_ADDRESS = "alice@example.com"
private const val SENDER_NAME = "Alice"
private const val RECIPIENT_ADDRESS = "bob@example.com"
private const val RECIPIENT_NAME = "Bob"

class NotificationContentCreatorTest : RobolectricTest() {

    private val accountId = AccountIdFactory.create()
    private val contactRepository = createFakeContentRepository()
    private val resourceProvider = TestNotificationResourceProvider()
    private val contentCreator = createNotificationContentCreator()
    private val messageReference = createMessageReference()
    private val message = createFakeLocalMessage(messageReference)

    @Test
    fun createFromMessage_withRegularMessage() {
        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.messageReference).isEqualTo(messageReference)
        assertThat(content.sender.personal).isEqualTo(SENDER_NAME)
        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo("$SUBJECT\n$PREVIEW")
        assertThat(content.summary.toString()).isEqualTo("$SENDER_NAME $SUBJECT")
    }

    @Test
    fun createFromMessage_withoutSubject() {
        stubbing(message) {
            on { subject } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.subject).isEqualTo("(No subject)")
        assertThat(content.preview.toString()).isEqualTo(PREVIEW)
        assertThat(content.summary.toString()).isEqualTo("$SENDER_NAME (No subject)")
    }

    @Test
    fun createFromMessage_withoutPreview() {
        stubbing(message) {
            on { previewType } doReturn PreviewType.NONE
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo(SUBJECT)
    }

    @Test
    fun createFromMessage_withErrorPreview() {
        stubbing(message) {
            on { previewType } doReturn PreviewType.ERROR
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo(SUBJECT)
    }

    @Test
    fun createFromMessage_withEncryptedMessage() {
        stubbing(message) {
            on { previewType } doReturn PreviewType.ENCRYPTED
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo("$SUBJECT\n*Encrypted*")
    }

    @Test
    fun createFromMessage_withoutSender() {
        stubbing(message) {
            on { from } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.sender.personal).isEqualTo("No sender")
        assertThat(content.summary.toString()).isEqualTo("No sender $SUBJECT")
    }

    @Test
    fun `isAnIdentity returns true for matching identity email`() {
        val account = createFakeAccount(identities = listOf(createLegacyAccountIdentity()))

        assertThat(account.isAnIdentity(Address(SENDER_ADDRESS, SENDER_NAME))).isTrue()
    }

    @Test
    fun `isAnIdentity returns false when no identity matches`() {
        val account = createFakeAccount(identities = listOf(createLegacyAccountIdentity()))

        assertThat(account.isAnIdentity(Address(RECIPIENT_ADDRESS, RECIPIENT_NAME))).isFalse()
    }

    @Test
    fun createFromMessage_withMessageFromSelf() {
        val content = contentCreator.createFromMessage(message, isFromSelf = true)

        assertThat(content.sender.personal).isEqualTo("To:Bob")
        assertThat(content.summary.toString()).isEqualTo("To:Bob $SUBJECT")
    }

    @Test
    fun createFromMessage_withoutEmptyMessage() {
        stubbing(message) {
            on { from } doReturn null
            on { subject } doReturn null
            on { previewType } doReturn PreviewType.NONE
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(message, isFromSelf = false)

        assertThat(content.sender.personal).isEqualTo("No sender")
        assertThat(content.subject).isEqualTo("(No subject)")
        assertThat(content.preview.toString()).isEqualTo("(No subject)")
        assertThat(content.summary.toString()).isEqualTo("No sender (No subject)")
    }

    private fun createNotificationContentCreator(): NotificationContentCreator {
        return NotificationContentCreator(
            resourceProvider,
            contactRepository,
            messageListPreferencesManager = mock {
                on { getConfig() } doReturn DisplayMessageListSettings()
            },
        )
    }

    private fun createFakeAccount(identities: List<Identity> = emptyList()): LegacyAccount {
        return LegacyAccount(
            id = accountId,
            name = "Test Account",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "Test Account",
                color = -1,
                avatar = AvatarDto(
                    id = accountId,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "TA",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = ServerSettings(
                type = "imap",
                host = "host",
                port = 993,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),
            outgoingServerSettings = ServerSettings(
                type = "smtp",
                host = "host",
                port = 465,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),
            identities = identities.ifEmpty { listOf(Identity(email = "user@example.com")) },
        )
    }

    private fun createLegacyAccountIdentity(): Identity = Identity(
        email = SENDER_ADDRESS,
        name = SENDER_NAME,
    )

    private fun createFakeContentRepository(): ContactRepository = mock()

    private fun createMessageReference(): MessageReference {
        return MessageReference(accountId, FOLDER_ID, UID)
    }

    private fun createFakeLocalMessage(messageReference: MessageReference): LocalMessage {
        return mock {
            on { makeMessageReference() } doReturn messageReference
            on { previewType } doReturn PreviewType.TEXT
            on { preview } doReturn PREVIEW
            on { subject } doReturn SUBJECT
            on { from } doReturn arrayOf(Address(SENDER_ADDRESS, SENDER_NAME))
            on { getRecipients(RecipientType.TO) } doReturn arrayOf(Address(RECIPIENT_ADDRESS, RECIPIENT_NAME))
        }
    }
}
