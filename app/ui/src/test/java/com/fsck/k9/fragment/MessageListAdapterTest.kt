package com.fsck.k9.fragment

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.FontSizes
import com.fsck.k9.FontSizes.FONT_DEFAULT
import com.fsck.k9.FontSizes.LARGE
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.provider.EmailProvider.MessageColumns
import com.fsck.k9.provider.EmailProvider.SpecialColumns
import com.fsck.k9.provider.EmailProvider.ThreadColumns
import com.fsck.k9.textString
import com.fsck.k9.ui.ContactBadge
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.nhaarman.mockito_kotlin.anyArray
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.mockito.AdditionalMatchers.aryEq
import org.robolectric.RuntimeEnvironment


private const val SOME_ACCOUNT_UUID = "6b84207b-25de-4dab-97c3-953bbf03fec6"
private const val DISPLAY_NAME = "Display Name"
private const val FIRST_LINE_DEFAULT_FONT_SIZE = 18f
private const val SECOND_LINE_DEFAULT_FONT_SIZE = 14f
private const val DATE_DEFAULT_FONT_SIZE = 14f

class MessageListAdapterTest : RobolectricTest() {
    val context: Context = ContextThemeWrapper(RuntimeEnvironment.application, R.style.Theme_K9_Light)
    val testAccount = Account(SOME_ACCOUNT_UUID)

    val messageHelper: MessageHelper = mock {
        on { getDisplayName(eq(testAccount), anyArray(), anyArray()) } doReturn DISPLAY_NAME
    }
    val preferences: Preferences = mock {
        on { getAccount(SOME_ACCOUNT_UUID) } doReturn testAccount
    }
    val contactsPictureLoader: ContactPictureLoader = mock()
    val listItemListener: MessageListItemActionListener = mock()


    @Test
    fun withShowAccountChip_shouldShowAccountChip() {
        val adapter = createAdapter(showAccountChip = true)

        val view = adapter.createAndBindView()

        assertTrue(view.accountChipView.isVisible)
    }

    @Test
    fun withoutShowAccountChip_shouldHideAccountChip() {
        val adapter = createAdapter(showAccountChip = false)

        val view = adapter.createAndBindView()

        assertTrue(view.accountChipView.isGone)
    }

    @Test
    fun withoutStars_shouldHideStarCheckBox() {
        val adapter = createAdapter(stars = false)

        val view = adapter.createAndBindView()

        assertTrue(view.starView.isGone)
    }

    @Test
    fun withStars_shouldShowStarCheckBox() {
        val adapter = createAdapter(stars = true)

        val view = adapter.createAndBindView()

        assertTrue(view.starView.isVisible)
    }

    @Test
    fun withStarsAndFlaggedMessage_shouldCheckStarCheckBox() {
        val adapter = createAdapter(stars = true)
        val cursor = createCursor(flagged = 1)

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.starView.isChecked)
    }

    @Test
    fun withStarsAndUnflaggedMessage_shouldNotCheckStarCheckBox() {
        val adapter = createAdapter(stars = true)
        val cursor = createCursor(flagged = 0)

        val view = adapter.createAndBindView(cursor)

        assertFalse(view.starView.isChecked)
    }

    @Test
    fun withoutShowContactPicture_shouldHideContactPictureView() {
        val adapter = createAdapter(showContactPicture = false)

        val view = adapter.createAndBindView()

        assertTrue(view.contactPictureView.isGone)
    }

    @Test
    fun withShowContactPicture_shouldShowContactPictureView() {
        val adapter = createAdapter(showContactPicture = true)

        val view = adapter.createAndBindView()

        assertTrue(view.contactPictureView.isVisible)
    }

    @Test
    fun withThreadCountOne_shouldHideThreadCountView() {
        val adapter = createAdapter()
        val cursor = createCursor(threadCount = 1)

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.threadCountView.isGone)
    }

    @Test
    fun withThreadCountGreaterOne_shouldShowThreadCountViewWithExpectedValue() {
        val adapter = createAdapter()
        val cursor = createCursor(threadCount = 13)

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.threadCountView.isVisible)
        assertEquals("13", view.threadCountView.textString)
    }

    @Test
    fun withoutSenderAboveSubject_shouldShowSubjectInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val cursor = createCursor(subject = "Subject")

        val view = adapter.createAndBindView(cursor)

        assertEquals("Subject", view.firstLineView.textString)
    }

    @Test
    fun withSenderAboveSubject_shouldShowDisplayNameInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)

        val view = adapter.createAndBindView()

        assertEquals(DISPLAY_NAME, view.firstLineView.textString)
    }

    @Test
    fun withoutSenderAboveSubjectAndZeroPreviewLines_shouldShowDisplayNameInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 0)

        val view = adapter.createAndBindView()

        assertEquals(DISPLAY_NAME, view.secondLineView.textString)
    }

    @Test
    fun withoutSenderAboveSubjectAndPreviewLines_shouldShowDisplayNameAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 1)
        val cursor = createCursor(preview = "Preview")

        val view = adapter.createAndBindView(cursor)

        assertEquals(secondLine(DISPLAY_NAME, "Preview"), view.secondLineView.textString)
    }

    @Test
    fun withSenderAboveSubjectAndZeroPreviewLines_shouldShowSubjectInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 0)
        val cursor = createCursor(subject = "Subject")

        val view = adapter.createAndBindView(cursor)

        assertEquals("Subject", view.secondLineView.textString)
    }

    @Test
    fun withSenderAboveSubjectAndPreviewLines_shouldShowSubjectAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 1)
        val cursor = createCursor(subject = "Subject", preview = "Preview")

        val view = adapter.createAndBindView(cursor)

        assertEquals(secondLine("Subject", "Preview"), view.secondLineView.textString)
    }

    @Test
    @Ignore("Currently failing. See issue #4152.")
    fun withSenderAboveSubjectAndMessageToMe_shouldDisplayIndicatorInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val cursor = createCursor(to = "to@domain.example")
        configureMessageHelperMockToMe("to@domain.example")

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.firstLineView.containsToMeIndicator())
    }

    @Test
    @Ignore("Currently failing. See issue #4152.")
    fun withSenderAboveSubjectAndMessageCcMe_shouldDisplayIndicatorInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val cursor = createCursor(cc = "cc@domain.example")
        configureMessageHelperMockToMe("cc@domain.example")

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.firstLineView.containsCcMeIndicator())
    }

    @Test
    fun withoutSenderAboveSubjectAndMessageToMe_shouldDisplayIndicatorInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val cursor = createCursor(to = "to@domain.example")
        configureMessageHelperMockToMe("to@domain.example")

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.secondLineView.containsToMeIndicator())
    }

    @Test
    fun withoutSenderAboveSubjectAndMessageCcMe_shouldDisplayIndicatorInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val cursor = createCursor(cc = "cc@domain.example")
        configureMessageHelperMockToMe("cc@domain.example")

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.secondLineView.containsCcMeIndicator())
    }

    @Test
    fun withAttachmentCountZero_shouldHideAttachmentCountView() {
        val adapter = createAdapter()
        val cursor = createCursor(attachmentCount = 0)

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.attachmentCountView.isGone)
    }

    @Test
    fun withNonZeroAttachmentCount_shouldShowAttachmentCountView() {
        val adapter = createAdapter()
        val cursor = createCursor(attachmentCount = 3)

        val view = adapter.createAndBindView(cursor)

        assertTrue(view.attachmentCountView.isVisible)
    }

    @Test
    fun withoutSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(subject = FONT_DEFAULT),
                senderAboveSubject = false
        )

        val view = adapter.createAndBindView()

        assertEquals(FIRST_LINE_DEFAULT_FONT_SIZE, view.firstLineView.textSize)
    }

    @Test
    fun withoutSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(subject = LARGE),
                senderAboveSubject = false
        )

        val view = adapter.createAndBindView()

        assertEquals(22f, view.firstLineView.textSize)
    }

    @Test
    fun withSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(sender = FONT_DEFAULT),
                senderAboveSubject = true
        )

        val view = adapter.createAndBindView()

        assertEquals(FIRST_LINE_DEFAULT_FONT_SIZE, view.firstLineView.textSize)
    }

    @Test
    fun withSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(sender = LARGE),
                senderAboveSubject = true
        )

        val view = adapter.createAndBindView()

        assertEquals(22f, view.firstLineView.textSize)
    }

    @Test
    fun withoutSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(sender = FONT_DEFAULT),
                senderAboveSubject = false
        )

        val view = adapter.createAndBindView()

        assertNull(view.secondLineView.getFirstAbsoluteSizeSpanValueOrNull())
    }

    @Test
    fun withoutSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(sender = LARGE),
                senderAboveSubject = false
        )

        val view = adapter.createAndBindView()

        assertEquals(22, view.secondLineView.getFirstAbsoluteSizeSpanValueOrNull())
    }

    @Test
    fun withSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(subject = FONT_DEFAULT),
                senderAboveSubject = true
        )

        val view = adapter.createAndBindView()

        assertNull(view.secondLineView.getFirstAbsoluteSizeSpanValueOrNull())
    }

    @Test
    fun withSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(subject = LARGE),
                senderAboveSubject = true
        )

        val view = adapter.createAndBindView()

        assertEquals(22, view.secondLineView.getFirstAbsoluteSizeSpanValueOrNull())
    }

    @Test
    fun dateWithDefaultFontSize_shouldNotSetTextSizeOfDateView() {
        val adapter = createAdapter(fontSizes = createFontSizes(date = FONT_DEFAULT))

        val view = adapter.createAndBindView()

        assertEquals(DATE_DEFAULT_FONT_SIZE, view.dateView.textSize)
    }

    @Test
    fun dateWithNonDefaultFontSize_shouldSetTextSizeOfDateView() {
        val adapter = createAdapter(fontSizes = createFontSizes(date = LARGE))

        val view = adapter.createAndBindView()

        assertEquals(22f, view.dateView.textSize)
    }

    @Test
    fun previewWithDefaultFontSize_shouldNotSetTextSizeOfSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(preview = FONT_DEFAULT),
                previewLines = 1
        )

        val view = adapter.createAndBindView()

        assertEquals(SECOND_LINE_DEFAULT_FONT_SIZE, view.secondLineView.textSize)
    }

    @Test
    fun previewWithNonDefaultFontSize_shouldSetTextSizeOfSecondLineView() {
        val adapter = createAdapter(
                fontSizes = createFontSizes(preview = LARGE),
                previewLines = 1
        )

        val view = adapter.createAndBindView()

        assertEquals(22f, view.secondLineView.textSize)
    }


    fun configureMessageHelperMockToMe(address: String) {
        val addresses = Address.parse(address)
        whenever(messageHelper.toMe(eq(testAccount), aryEq(addresses))).thenReturn(true)
    }

    fun createFontSizes(
            subject: Int = FONT_DEFAULT,
            sender: Int = FONT_DEFAULT,
            preview: Int = FONT_DEFAULT,
            date: Int = FONT_DEFAULT
    ): FontSizes {
        return FontSizes().apply {
            messageListSubject = subject
            messageListSender = sender
            messageListPreview = preview
            messageListDate = date
        }
    }

    fun createAdapter(
            fontSizes: FontSizes = createFontSizes(),
            previewLines: Int = 0,
            stars: Boolean = true,
            senderAboveSubject: Boolean = false,
            showContactPicture: Boolean = true,
            showingThreadedList: Boolean = true,
            backGroundAsReadIndicator: Boolean = false,
            showAccountChip: Boolean = false
    ): MessageListAdapter {
        val appearance = MessageListAppearance(
                fontSizes,
                previewLines,
                stars,
                senderAboveSubject,
                showContactPicture,
                showingThreadedList,
                backGroundAsReadIndicator,
                showAccountChip
        )

        return MessageListAdapter(
                context = context,
                theme = context.theme,
                res = context.resources,
                layoutInflater = LayoutInflater.from(context),
                messageHelper = messageHelper,
                contactsPictureLoader = contactsPictureLoader,
                preferences = preferences,
                listItemListener = listItemListener,
                appearance = appearance
        )
    }

    fun createCursor(
            id: Long = 0L,
            uid: String = "irrelevant",
            internalDate: Long = 0L,
            subject: String =  "irrelevant",
            date: Long = 0L,
            sender: String = "irrelevant@domain.example",
            to: String = "irrelevant@domain.example",
            cc: String = "irrelevant@domain.example",
            read: Int = 0,
            flagged: Int = 0,
            answered: Int = 0,
            forwarded: Int = 0,
            attachmentCount: Int = 0,
            folderId: String = "irrelevant",
            previewType: String = "text",
            preview: String = "irrelevant",
            threadRoot: Long = 0L,
            accountUuid: String = SOME_ACCOUNT_UUID,
            folderServerId: String = "irrelevant",
            threadCount: Int = 0
    ): Cursor {
        val mapping = mapOf(
                MessageColumns.ID to id,
                MessageColumns.UID to uid,
                MessageColumns.INTERNAL_DATE to internalDate,
                MessageColumns.SUBJECT to subject,
                MessageColumns.DATE to date,
                MessageColumns.SENDER_LIST to Address.pack(Address.parse(sender)),
                MessageColumns.TO_LIST to Address.pack(Address.parse(to)),
                MessageColumns.CC_LIST to Address.pack(Address.parse(cc)),
                MessageColumns.READ to read,
                MessageColumns.FLAGGED to flagged,
                MessageColumns.ANSWERED to answered,
                MessageColumns.FORWARDED to forwarded,
                MessageColumns.ATTACHMENT_COUNT to attachmentCount,
                MessageColumns.FOLDER_ID to folderId,
                MessageColumns.PREVIEW_TYPE to previewType,
                MessageColumns.PREVIEW to preview,
                ThreadColumns.ROOT to threadRoot,
                SpecialColumns.ACCOUNT_UUID to accountUuid,
                SpecialColumns.FOLDER_SERVER_ID to folderServerId,
                SpecialColumns.THREAD_COUNT to threadCount
        )
        return MatrixCursor(mapping.keys.toTypedArray())
                .apply { addRow(mapping.values.toTypedArray()) }
                .also { it.moveToFirst() }
    }

    fun MessageListAdapter.createAndBindView(cursor: Cursor = createCursor()): View {
        val view = newView(context, cursor, LinearLayout(context))
        bindView(view, context, cursor)

        return view
    }

    fun secondLine(senderOrSubject: String, preview: String)= "$senderOrSubject $preview"

    val View.accountChipView: View get() = findViewById(R.id.account_color_chip)
    val View.starView: CheckBox get() = findViewById(R.id.star)
    val View.contactPictureView: ContactBadge get() = findViewById(R.id.contact_badge)
    val View.threadCountView: TextView get() = findViewById(R.id.thread_count)
    val View.firstLineView: TextView get() = findViewById(R.id.subject)
    val View.secondLineView: TextView get() = findViewById(R.id.preview)
    val View.attachmentCountView: View get() = findViewById(R.id.attachment)
    val View.dateView: TextView get() = findViewById(R.id.date)

    fun TextView.containsToMeIndicator() = textString.startsWith("»")
    fun TextView.containsCcMeIndicator() = textString.startsWith("›")

    fun TextView.getFirstAbsoluteSizeSpanValueOrNull(): Int? {
        val spans = (text as Spannable).getSpans(0, text.length, AbsoluteSizeSpan::class.java)
        return spans.firstOrNull()?.size
    }
}
