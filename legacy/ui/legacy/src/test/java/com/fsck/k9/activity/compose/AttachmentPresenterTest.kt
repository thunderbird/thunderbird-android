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
private val URI = Uri.Builder().scheme("content://").build()

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

    private fun mockLoaderManager(attachmentSupplier: Supplier<Attachment>) {
        doAnswer {
            val loaderCallbacks = it.getArgument<LoaderCallbacks<Attachment>>(2)
            loaderCallbacks.onLoadFinished(mock(), attachmentSupplier.get().deriveWithLoadComplete(PATH_TO_FILE))
            null
        }.whenever(loaderManager).initLoader(anyInt(), any(), any<LoaderCallbacks<Attachment>>())
    }
}
