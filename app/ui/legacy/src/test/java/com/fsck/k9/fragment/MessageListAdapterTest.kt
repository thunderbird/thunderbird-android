package com.fsck.k9.fragment

import android.content.Context
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.FontSizes
import com.fsck.k9.FontSizes.FONT_DEFAULT
import com.fsck.k9.FontSizes.LARGE
import com.fsck.k9.RobolectricTest
import com.fsck.k9.TestClock
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.mail.Address
import com.fsck.k9.textString
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.Robolectric

private const val SOME_ACCOUNT_UUID = "6b84207b-25de-4dab-97c3-953bbf03fec6"
private const val FIRST_LINE_DEFAULT_FONT_SIZE = 16f
private const val SECOND_LINE_DEFAULT_FONT_SIZE = 14f
private const val DATE_DEFAULT_FONT_SIZE = 14f

class MessageListAdapterTest : RobolectricTest() {
    val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
    val context: Context = ContextThemeWrapper(activity, R.style.Theme_K9_Light)

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
    fun withStarsAndStarredMessage_shouldCheckStarCheckBox() {
        val adapter = createAdapter(stars = true)
        val messageListItem = createMessageListItem(isStarred = true)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.starView.isChecked)
    }

    @Test
    fun withStarsAndUnstarredMessage_shouldNotCheckStarCheckBox() {
        val adapter = createAdapter(stars = true)
        val messageListItem = createMessageListItem(isStarred = false)

        val view = adapter.createAndBindView(messageListItem)

        assertFalse(view.starView.isChecked)
    }

    @Test
    fun withoutShowContactPicture_shouldHideContactPictureView() {
        val adapter = createAdapter(showContactPicture = false)

        val view = adapter.createAndBindView()

        assertTrue(view.contactPictureContainerView.isGone)
    }

    @Test
    fun withShowContactPicture_shouldShowContactPictureView() {
        val adapter = createAdapter(showContactPicture = true)

        val view = adapter.createAndBindView()

        assertTrue(view.contactPictureContainerView.isVisible)
    }

    @Test
    fun withThreadCountOne_shouldHideThreadCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(threadCount = 1)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.threadCountView.isGone)
    }

    @Test
    fun withThreadCountGreaterOne_shouldShowThreadCountViewWithExpectedValue() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(threadCount = 13)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.threadCountView.isVisible)
        assertEquals("13", view.threadCountView.textString)
    }

    @Test
    fun withoutSenderAboveSubject_shouldShowSubjectInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = "Subject")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals("Subject", view.firstLineView.textString)
    }

    @Test
    fun withSenderAboveSubject_shouldShowDisplayNameInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val messageListItem = createMessageListItem(displayName = "Display Name")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals("Display Name", view.firstLineView.textString)
    }

    @Test
    fun withoutSenderAboveSubjectAndZeroPreviewLines_shouldShowDisplayNameInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 0)
        val messageListItem = createMessageListItem(displayName = "Display Name")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals("Display Name", view.secondLineView.textString)
    }

    @Test
    fun withoutSenderAboveSubjectAndPreviewLines_shouldShowDisplayNameAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 1)
        val messageListItem = createMessageListItem(displayName = "Display Name", previewText = "Preview")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals(secondLine("Display Name", "Preview"), view.secondLineView.textString)
    }

    @Test
    fun withSenderAboveSubjectAndZeroPreviewLines_shouldShowSubjectInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 0)
        val messageListItem = createMessageListItem(subject = "Subject")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals("Subject", view.secondLineView.textString)
    }

    @Test
    fun withSenderAboveSubjectAndPreviewLines_shouldShowSubjectAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 1)
        val messageListItem = createMessageListItem(subject = "Subject", previewText = "Preview")

        val view = adapter.createAndBindView(messageListItem)

        assertEquals(secondLine("Subject", "Preview"), view.secondLineView.textString)
    }

    @Test
    fun withMissingSubject_shouldDisplayNoSubjectIndicator() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = null)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.firstLineView.containsNoSubjectIndicator())
    }

    @Test
    fun withEmptySubject_shouldDisplayNoSubjectIndicator() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = "")

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.firstLineView.containsNoSubjectIndicator())
    }

    @Test
    @Ignore("Currently failing. See issue #4152.")
    fun withSenderAboveSubjectAndMessageToMe_shouldDisplayIndicatorInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val messageListItem = createMessageListItem(toMe = true)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.firstLineView.containsToMeIndicator())
    }

    @Test
    @Ignore("Currently failing. See issue #4152.")
    fun withSenderAboveSubjectAndMessageCcMe_shouldDisplayIndicatorInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val messageListItem = createMessageListItem(ccMe = true)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.firstLineView.containsCcMeIndicator())
    }

    @Test
    fun withoutSenderAboveSubjectAndMessageToMe_shouldDisplayIndicatorInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(toMe = true)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.secondLineView.containsToMeIndicator())
    }

    @Test
    fun withoutSenderAboveSubjectAndMessageCcMe_shouldDisplayIndicatorInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(ccMe = true)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.secondLineView.containsCcMeIndicator())
    }

    @Test
    fun withoutAttachments_shouldHideAttachmentCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(hasAttachments = false)

        val view = adapter.createAndBindView(messageListItem)

        assertTrue(view.attachmentCountView.isGone)
    }

    @Test
    fun withAttachments_shouldShowAttachmentCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(hasAttachments = true)

        val view = adapter.createAndBindView(messageListItem)

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
            contactsPictureLoader = contactsPictureLoader,
            listItemListener = listItemListener,
            appearance = appearance,
            relativeDateTimeFormatter = RelativeDateTimeFormatter(context, TestClock())
        )
    }

    fun createMessageListItem(
        position: Int = 0,
        account: Account = Account(SOME_ACCOUNT_UUID),
        subject: String? = "irrelevant",
        threadCount: Int = 0,
        messageDate: Long = 0L,
        displayName: CharSequence = "irrelevant",
        displayAddress: Address? = Address.parse("irrelevant@domain.example").first(),
        toMe: Boolean = false,
        ccMe: Boolean = false,
        previewText: String = "irrelevant",
        isMessageEncrypted: Boolean = false,
        isRead: Boolean = false,
        isStarred: Boolean = false,
        isAnswered: Boolean = false,
        isForwarded: Boolean = false,
        hasAttachments: Boolean = false,
        uniqueId: Long = 0L,
        folderId: Long = 0L,
        messageUid: String = "irrelevant",
        databaseId: Long = 0L,
        threadRoot: Long = 0L
    ): MessageListItem {
        return MessageListItem(
            position,
            account,
            subject,
            threadCount,
            messageDate,
            displayName,
            displayAddress,
            toMe,
            ccMe,
            previewText,
            isMessageEncrypted,
            isRead,
            isStarred,
            isAnswered,
            isForwarded,
            hasAttachments,
            uniqueId,
            folderId,
            messageUid,
            databaseId,
            threadRoot
        )
    }

    fun MessageListAdapter.createAndBindView(item: MessageListItem = createMessageListItem()): View {
        messages = listOf(item)
        return getView(0, null, LinearLayout(context))
    }

    fun secondLine(senderOrSubject: String, preview: String) = "$senderOrSubject $preview"

    val View.accountChipView: View get() = findViewById(R.id.account_color_chip)
    val View.starView: CheckBox get() = findViewById(R.id.star)
    val View.contactPictureContainerView: View get() = findViewById(R.id.contact_picture_container)
    val View.threadCountView: TextView get() = findViewById(R.id.thread_count)
    val View.firstLineView: TextView get() = findViewById(R.id.subject)
    val View.secondLineView: TextView get() = findViewById(R.id.preview)
    val View.attachmentCountView: View get() = findViewById(R.id.attachment)
    val View.dateView: TextView get() = findViewById(R.id.date)

    fun TextView.containsToMeIndicator() = textString.startsWith("»")
    fun TextView.containsCcMeIndicator() = textString.startsWith("›")
    fun TextView.containsNoSubjectIndicator() = textString.contains(context.getString(R.string.general_no_subject))

    fun TextView.getFirstAbsoluteSizeSpanValueOrNull(): Int? {
        val spans = (text as Spannable).getSpans(0, text.length, AbsoluteSizeSpan::class.java)
        return spans.firstOrNull()?.size
    }
}
