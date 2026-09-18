package com.fsck.k9.activity.compose

import android.net.Uri
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.compose.AttachmentPresenter.AttachmentMvpView
import com.fsck.k9.activity.compose.AttachmentPresenter.AttachmentsChangedListener
import com.fsck.k9.activity.misc.Attachment
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.LocalBodyPart
import com.fsck.k9.mailstore.MessageViewInfo
import java.util.function.Supplier
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private val attachmentMvpView = mock<AttachmentMvpView>()
private val loaderManager = mock<LoaderManager>()
private val listener = mock<AttachmentsChangedListener>()
private val attachmentResolver = mock<AttachmentResolver>()

private const val ACCOUNT_UUID = "uuid"
private const val SUBJECT = "subject"
private const val TEXT = "text"
private const val EXTRA_TEXT = "extra text"
private const val ATTACHMENT_NAME = "1x1.png"
private const val MESSAGE_ID = 1L
private const val PATH_TO_FILE = "path/to/file.png"
private const val MIME_TYPE = "image/png"
private const val SECOND_ATTACHMENT_NAME = "2x2.png"
private const val THIRD_ATTACHMENT_NAME = "3x3.png"
private const val CONTENT_ID = "xyz"
private const val SIZE = 42L
private val URI = Uri.Builder().scheme("content://").build()
private val SECOND_URI = Uri.Builder().scheme("content://").appendPath("second").build()
private val THIRD_URI = Uri.Builder().scheme("content://").appendPath("third").build()

class AttachmentPresenterTest : K9RobolectricTest() {
    lateinit var attachmentPresenter: AttachmentPresenter

    @Before
    fun setUp() {
        attachmentPresenter = AttachmentPresenter(
            ApplicationProvider.getApplicationContext(),
            attachmentMvpView,
            loaderManager,
            listener,
        )
    }

    @Test
    fun loadNonInlineAttachments_normalAttachment() {
        val size = 42L
        val message = MimeMessage()
        MimeMessageHelper.setBody(message, TextBody(TEXT))
        val attachmentViewInfo = AttachmentViewInfo(
            MIME_TYPE,
            ATTACHMENT_NAME,
            size,
            URI,
            false,
            LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, size),
            true,
        )
        val messageViewInfo = MessageViewInfo(
            message, false, message, SUBJECT, false, TEXT, TEXT, listOf(attachmentViewInfo), null, attachmentResolver,
            EXTRA_TEXT, ArrayList(), null,
        )

        mockLoaderManager({ attachmentPresenter.attachments.get(0) as Attachment })

        val result = attachmentPresenter.loadAllAvailableAttachments(messageViewInfo)

        assertThat(result).isTrue()
        assertThat(attachmentPresenter.attachments).hasSize(1)
        assertThat(attachmentPresenter.inlineAttachments).isEmpty()
        val attachment = attachmentPresenter.attachments.get(0)
        assertThat(attachment?.name).isEqualTo(ATTACHMENT_NAME)
        assertThat(attachment?.size).isEqualTo(size)
        assertThat(attachment?.state).isEqualTo(com.fsck.k9.message.Attachment.LoadingState.COMPLETE)
        assertThat(attachment?.fileName).isEqualTo(PATH_TO_FILE)
    }

    @Test
    fun loadNonInlineAttachments_normalAttachmentNotAvailable() {
        val size = 42L
        val message = MimeMessage()
        MimeMessageHelper.setBody(message, TextBody(TEXT))
        val attachmentViewInfo = AttachmentViewInfo(
            MIME_TYPE,
            ATTACHMENT_NAME,
            size,
            URI,
            false,
            LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, size),
            false,
        )
        val messageViewInfo = MessageViewInfo(
            message, false, message, SUBJECT, false, TEXT, TEXT, listOf(attachmentViewInfo), null, attachmentResolver,
            EXTRA_TEXT, ArrayList(), null,
        )

        val result = attachmentPresenter.loadAllAvailableAttachments(messageViewInfo)

        assertThat(result).isFalse()
        assertThat(attachmentPresenter.attachments).isEmpty()
        assertThat(attachmentPresenter.inlineAttachments).isEmpty()
    }

    @Test
    fun loadNonInlineAttachments_inlineAttachment() {
        val size = 42L
        val contentId = "xyz"
        val message = MimeMessage()
        MimeMessageHelper.setBody(message, TextBody(TEXT))
        val localBodyPart = LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, size)
        localBodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, contentId)
        val attachmentViewInfo = AttachmentViewInfo(MIME_TYPE, ATTACHMENT_NAME, size, URI, true, localBodyPart, true)
        val messageViewInfo = MessageViewInfo(
            message, false, message, SUBJECT, false, TEXT, TEXT, listOf(attachmentViewInfo), null, attachmentResolver,
            EXTRA_TEXT, ArrayList(), null,
        )

        mockLoaderManager({ attachmentPresenter.inlineAttachments.get(contentId) as Attachment })

        val result = attachmentPresenter.loadAllAvailableAttachments(messageViewInfo)

        assertThat(result).isTrue()
        assertThat(attachmentPresenter.attachments).isEmpty()
        assertThat(attachmentPresenter.inlineAttachments).hasSize(1)
        val attachment = attachmentPresenter.inlineAttachments.get(contentId)
        assertThat(attachment?.name).isEqualTo(ATTACHMENT_NAME)
        assertThat(attachment?.size).isEqualTo(size)
        assertThat(attachment?.state).isEqualTo(com.fsck.k9.message.Attachment.LoadingState.COMPLETE)
        assertThat(attachment?.fileName).isEqualTo(PATH_TO_FILE)
    }

    @Test
    fun loadNonInlineAttachments_inlineAttachmentNotAvailable() {
        val size = 42L
        val contentId = "xyz"
        val message = MimeMessage()
        MimeMessageHelper.setBody(message, TextBody(TEXT))
        val localBodyPart = LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, size)
        localBodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, contentId)
        val attachmentViewInfo = AttachmentViewInfo(MIME_TYPE, ATTACHMENT_NAME, size, URI, true, localBodyPart, false)
        val messageViewInfo = MessageViewInfo(
            message, false, message, SUBJECT, false, TEXT, TEXT, listOf(attachmentViewInfo), null, attachmentResolver,
            EXTRA_TEXT, ArrayList(), null,
        )

        val result = attachmentPresenter.loadAllAvailableAttachments(messageViewInfo)

        assertThat(result).isFalse()
        assertThat(attachmentPresenter.attachments).isEmpty()
        assertThat(attachmentPresenter.inlineAttachments).isEmpty()
    }

    @Test
    fun `processDraftMessage should not request download when all parts are available`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(availableAttachment(ATTACHMENT_NAME))
        mockLoaderManager({ testSubject.attachments.get(0) as Attachment })

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(0)
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(0)
        assertThat(testSubject.attachments).hasSize(1)
    }

    @Test
    fun `processDraftMessage should request download when a part is missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(missingAttachment(ATTACHMENT_NAME))

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(0)
    }

    @Test
    fun `processDraftMessage should request download when only some parts are available`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            missingAttachment(SECOND_ATTACHMENT_NAME),
        )
        mockLoaderManager({ testSubject.attachments.get(0) as Attachment })

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(testSubject.attachments).hasSize(1)
    }

    @Test
    fun `processDraftMessage should not add an attachment twice when called again after download`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val firstPass = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            missingAttachment(SECOND_ATTACHMENT_NAME),
        )
        val secondPass = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            availableAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
        )
        mockLoaderManager({ firstAttachmentWithMetadata(testSubject) })
        testSubject.processDraftMessage(firstPass)

        // Act
        testSubject.processDraftMessage(secondPass)

        // Assert
        assertThat(testSubject.attachments).hasSize(2)
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
    }

    @Test
    fun `processDraftMessage should do nothing when the draft has no attachments`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith()

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(0)
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(0)
        assertThat(testSubject.attachments).isEmpty()
    }

    @Test
    fun `processDraftMessage should request download when an inline part is missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val localBodyPart = LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, SIZE)
        localBodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, CONTENT_ID)
        val inlineAttachment =
            AttachmentViewInfo(MIME_TYPE, ATTACHMENT_NAME, SIZE, URI, true, localBodyPart, false)
        val messageViewInfo = messageViewInfoWith(inlineAttachment)

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
    }

    @Test
    fun `processDraftMessage should warn instead of downloading again when parts are still missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(missingAttachment(ATTACHMENT_NAME))
        testSubject.processDraftMessage(messageViewInfo)

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(1)
    }

    @Test
    fun `processMessageToForward should still warn and not request a download`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(missingAttachment(ATTACHMENT_NAME))

        // Act
        testSubject.processMessageToForward(messageViewInfo)

        // Assert
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(1)
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(0)
    }

    @Test
    fun `processDraftMessage should request download when one of three attachments is missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            availableAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
            missingAttachment(THIRD_ATTACHMENT_NAME, THIRD_URI),
        )
        mockLoaderManager({ firstAttachmentWithMetadata(testSubject) })

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(testSubject.attachments).hasSize(2)
    }

    @Test
    fun `processDraftMessage should request download when both a normal and an inline part are missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(
            missingAttachment(ATTACHMENT_NAME),
            missingInlineAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
        )

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(testSubject.attachments).isEmpty()
        assertThat(testSubject.inlineAttachments).isEmpty()
    }

    @Test
    fun `processDraftMessage should request download when only the inline part is missing`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val messageViewInfo = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            missingInlineAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
        )
        mockLoaderManager({ firstAttachmentWithMetadata(testSubject) })

        // Act
        testSubject.processDraftMessage(messageViewInfo)

        // Assert
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(testSubject.attachments).hasSize(1)
        assertThat(testSubject.inlineAttachments).isEmpty()
    }

    @Test
    fun `processDraftMessage should stay stable when called a third time`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val incomplete = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            missingAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
        )
        val complete = messageViewInfoWith(
            availableAttachment(ATTACHMENT_NAME),
            availableAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI),
        )
        mockLoaderManager({ firstAttachmentWithMetadata(testSubject) })
        testSubject.processDraftMessage(incomplete)
        testSubject.processDraftMessage(complete)

        // Act
        testSubject.processDraftMessage(complete)

        // Assert
        assertThat(testSubject.attachments).hasSize(2)
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(0)
    }

    @Test
    fun `processMessageToForward should be unaffected by a previous draft download request`() {
        // Arrange
        val fakeView = FakeAttachmentMvpView()
        val testSubject = createPresenter(fakeView)
        val draft = messageViewInfoWith(missingAttachment(ATTACHMENT_NAME))
        val toForward = messageViewInfoWith(missingAttachment(SECOND_ATTACHMENT_NAME, SECOND_URI))
        testSubject.processDraftMessage(draft)

        // Act
        testSubject.processMessageToForward(toForward)

        // Assert
        assertThat(fakeView.missingAttachmentsWarningCount).isEqualTo(1)
        assertThat(fakeView.downloadCompleteMessageCount).isEqualTo(1)
    }

    private fun firstAttachmentWithMetadata(presenter: AttachmentPresenter): Attachment {
        return presenter.attachments.first {
            it?.state == com.fsck.k9.message.Attachment.LoadingState.METADATA
        } as Attachment
    }

    private fun missingInlineAttachment(name: String, uri: Uri = URI): AttachmentViewInfo {
        val localBodyPart = LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, SIZE)
        localBodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, name)
        return AttachmentViewInfo(MIME_TYPE, name, SIZE, uri, true, localBodyPart, false)
    }

    private fun createPresenter(view: AttachmentMvpView): AttachmentPresenter {
        return AttachmentPresenter(
            ApplicationProvider.getApplicationContext(),
            view,
            loaderManager,
            listener,
        )
    }

    private fun availableAttachment(name: String, uri: Uri = URI): AttachmentViewInfo {
        return AttachmentViewInfo(
            MIME_TYPE,
            name,
            SIZE,
            uri,
            false,
            LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, SIZE),
            true,
        )
    }

    private fun missingAttachment(name: String, uri: Uri = URI): AttachmentViewInfo {
        return AttachmentViewInfo(
            MIME_TYPE,
            name,
            SIZE,
            uri,
            false,
            LocalBodyPart(ACCOUNT_UUID, mock(), MESSAGE_ID, SIZE),
            false,
        )
    }

    private fun messageViewInfoWith(vararg attachments: AttachmentViewInfo): MessageViewInfo {
        val message = MimeMessage()
        MimeMessageHelper.setBody(message, TextBody(TEXT))
        return MessageViewInfo(
            message, false, message, SUBJECT, false, TEXT, TEXT, attachments.toList(), null, attachmentResolver,
            EXTRA_TEXT, ArrayList(), null,
        )
    }

    private fun mockLoaderManager(attachmentSupplier: Supplier<Attachment>) {
        doAnswer {
            val loaderCallbacks = it.getArgument<LoaderCallbacks<Attachment>>(2)
            loaderCallbacks.onLoadFinished(mock(), attachmentSupplier.get().deriveWithLoadComplete(PATH_TO_FILE))
            null
        }.whenever(loaderManager).initLoader(anyInt(), any(), any<LoaderCallbacks<Attachment>>())
    }
}

private class FakeAttachmentMvpView : AttachmentMvpView {
    var downloadCompleteMessageCount = 0
    var missingAttachmentsWarningCount = 0

    override fun showWaitingForAttachmentDialog(waitingAction: AttachmentPresenter.WaitingAction?) = Unit
    override fun dismissWaitingForAttachmentDialog() = Unit
    override fun showPickAttachmentDialog(requestCode: Int) = Unit
    override fun addAttachmentView(attachment: Attachment?) = Unit
    override fun removeAttachmentView(attachment: Attachment?) = Unit
    override fun updateAttachmentView(attachment: Attachment?) = Unit
    override fun performSendAfterChecks() = Unit
    override fun performSaveAfterChecks() = Unit

    override fun showMissingAttachmentsPartialMessageWarning() {
        missingAttachmentsWarningCount++
    }

    override fun showMissingAttachmentsPartialMessageForwardWarning() = Unit

    override fun downloadCompleteMessage() {
        downloadCompleteMessageCount++
    }
}
