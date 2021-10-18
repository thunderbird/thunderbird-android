package com.fsck.k9.notification

import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.extractors.PreviewResult.PreviewType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val ACCOUNT_UUID = "1-2-3"
private const val FOLDER_ID = 23L
private const val UID = "42"
private const val PREVIEW = "Message preview text"
private const val SUBJECT = "Message subject"
private const val SENDER_ADDRESS = "alice@example.com"
private const val SENDER_NAME = "Alice"
private const val RECIPIENT_ADDRESS = "bob@example.com"
private const val RECIPIENT_NAME = "Bob"

class NotificationContentCreatorTest : RobolectricTest() {
    private val resourceProvider = TestNotificationResourceProvider()
    private val contentCreator = createNotificationContentCreator()
    private val messageReference = createMessageReference()
    private val account = createFakeAccount()
    private val message = createFakeLocalMessage(messageReference)

    @Test
    fun createFromMessage_withRegularMessage() {
        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.messageReference).isEqualTo(messageReference)
        assertThat(content.sender).isEqualTo(SENDER_NAME)
        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo("$SUBJECT\n$PREVIEW")
        assertThat(content.summary.toString()).isEqualTo("$SENDER_NAME $SUBJECT")
    }

    @Test
    fun createFromMessage_withoutSubject() {
        stubbing(message) {
            on { subject } doReturn null
        }

        val content = contentCreator.createFromMessage(account, message)

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

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo(SUBJECT)
    }

    @Test
    fun createFromMessage_withErrorPreview() {
        stubbing(message) {
            on { previewType } doReturn PreviewType.ERROR
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo(SUBJECT)
    }

    @Test
    fun createFromMessage_withEncryptedMessage() {
        stubbing(message) {
            on { previewType } doReturn PreviewType.ENCRYPTED
            on { preview } doReturn null
        }

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.subject).isEqualTo(SUBJECT)
        assertThat(content.preview.toString()).isEqualTo("$SUBJECT\n*Encrypted*")
    }

    @Test
    fun createFromMessage_withoutSender() {
        stubbing(message) {
            on { from } doReturn null
        }

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.sender).isEqualTo("No sender")
        assertThat(content.summary.toString()).isEqualTo(SUBJECT)
    }

    @Test
    fun createFromMessage_withMessageFromSelf() {
        stubbing(account) {
            on { isAnIdentity(any<Array<Address>>()) } doReturn true
        }

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.sender).isEqualTo("To:Bob")
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

        val content = contentCreator.createFromMessage(account, message)

        assertThat(content.sender).isEqualTo("No sender")
        assertThat(content.subject).isEqualTo("(No subject)")
        assertThat(content.preview.toString()).isEqualTo("(No subject)")
        assertThat(content.summary.toString()).isEqualTo("(No subject)")
    }

    private fun createNotificationContentCreator(): NotificationContentCreator {
        return NotificationContentCreator(ApplicationProvider.getApplicationContext(), resourceProvider)
    }

    private fun createFakeAccount(): Account = mock()

    private fun createMessageReference(): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_ID, UID)
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
