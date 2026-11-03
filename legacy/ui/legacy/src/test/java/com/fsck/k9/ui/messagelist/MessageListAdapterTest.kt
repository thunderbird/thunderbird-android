package com.fsck.k9.ui.messagelist

import android.content.Context
import android.os.Looper
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.KeyEvent
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import com.fsck.k9.FontSizes
import com.fsck.k9.FontSizes.Companion.FONT_DEFAULT
import com.fsck.k9.FontSizes.Companion.LARGE
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.item.MessageViewHolder
import com.google.android.material.textview.MaterialTextView
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private val SOME_ACCOUNT_ID = AccountIdFactory.of("6b84207b-25de-4dab-97c3-953bbf03fec6")
private const val FIRST_LINE_DEFAULT_FONT_SIZE = 16f
private const val SECOND_LINE_DEFAULT_FONT_SIZE = 14f
private const val DATE_DEFAULT_FONT_SIZE = 14f

private val DATE_TIME_FORMAT = MessageListDateTimeFormat.Contextual

class MessageListAdapterTest : RobolectricTest() {
    private val activityController = Robolectric.buildActivity(AppCompatActivity::class.java)
    val activity = activityController.get().apply {
        setTheme(com.google.android.material.R.style.Theme_Material3_Light)
    }.also {
        activityController.setup().visible()
    }
    val context: Context = ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_Light)

    val contactsPictureLoader: ContactPictureLoader = mock()
    val listItemListener: MessageListItemActionListener = mock()

    @Test
    @Config(sdk = [26, 31, 36])
    fun `keypad mode should focus the message row and center should open that message`() {
        // Arrange
        val testSubject = createAdapter(keypadNavigation = { true })
        val message = createMessageListItem()
        val row = testSubject.createAndBindView(message)
        showRow(row)

        // Act
        assertThat(row.requestFocusFromTouch()).isTrue()
        row.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        row.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(row.findFocus()).isSameInstanceAs(row)
        assertThat(row.contactPictureContainerView.requestFocusFromTouch()).isFalse()
        assertThat(row.findViewById<View>(R.id.star_click_area).requestFocusFromTouch()).isFalse()
        verify(listItemListener).onMessageClicked(message)
    }

    @Test
    @Config(sdk = [26, 31, 36])
    fun `keypad mode should retain avatar and star touch actions`() {
        // Arrange
        val testSubject = createAdapter(keypadNavigation = { true })
        val message = createMessageListItem()
        val row = testSubject.createAndBindView(message)

        // Act
        row.contactPictureContainerView.performClick()
        row.findViewById<View>(R.id.star_click_area).performClick()

        // Assert
        verify(listItemListener).onToggleMessageSelection(message)
        verify(listItemListener).onToggleMessageFlag(message)
    }

    @Test
    @Config(sdk = [26, 31, 36])
    fun `disabling keypad mode should restore child keyboard focus on a reused row`() {
        // Arrange
        var keypadEnabled = true
        val testSubject = createAdapter(keypadNavigation = { keypadEnabled })
        val row = testSubject.createAndBindView()
        showRow(row)
        assertThat(row.contactPictureContainerView.requestFocusFromTouch()).isFalse()

        // Act
        keypadEnabled = false
        testSubject.onBindViewHolder(row.tag as MessageViewHolder, 0)

        // Assert
        assertThat(row.contactPictureContainerView.requestFocusFromTouch()).isTrue()
        assertThat(row.findFocus()).isSameInstanceAs(row.contactPictureContainerView)
    }

    private fun showRow(row: View) {
        activity.setContentView(row)
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, 480, 800)
        decor.dispatchWindowFocusChanged(true)
    }

    @Test
    @Config(sdk = [26, 31, 36])
    fun `returning from reader restores the exact third message and center opens it`() {
        // Arrange
        val testSubject = createAdapter(keypadNavigation = { true })
        val messages = (1..3).map { createMessageListItem(uniqueId = it.toLong(), messageUid = "$it") }
        testSubject.viewItems = messages.map { MessageListViewItem.Message(it) }
        val list = showMessages(testSubject)
        var ready = false
        val restoration = MessageListFocusRestorer(
            list,
            { reference -> testSubject.getItem(reference)?.let(testSubject::getPosition) },
            { ready },
        )
        restoration.revealAndFocus(messages[2].messageReference)
        settleList(list)
        assertThat(list.findFocus()).isSameInstanceAs(list)

        // Act
        ready = true
        restoration.retry()
        settleList(list)
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(list.focusedChild?.let(list::findContainingViewHolder)?.itemId).isEqualTo(messages[2].uniqueId)
        verify(listItemListener).onMessageClicked(messages[2])
        restoration.close()
    }

    private fun showMessages(adapter: MessageListAdapter): RecyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        itemAnimator = null
        this.adapter = adapter
        showRow(this)
        settleList(this)
    }

    private fun settleList(list: RecyclerView) {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            list.measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            )
            list.layout(0, 0, 480, 600)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun withShowAccountIndicator_shouldShowAccountIndicator() {
        val adapter = createAdapter(showAccountIndicator = true)

        val view = adapter.createAndBindView()

        assertThat(view.accountIndicatorView).isVisible()
    }

    @Test
    fun withoutShowAccountIndicator_shouldHideAccountIndicator() {
        val adapter = createAdapter(showAccountIndicator = false)

        val view = adapter.createAndBindView()

        assertThat(view.accountIndicatorView).isGone()
    }

    @Test
    fun withoutStars_shouldHideStarView() {
        val adapter = createAdapter(stars = false)

        val view = adapter.createAndBindView()

        assertThat(view.starView).isGone()
    }

    @Test
    fun withStars_shouldShowStarView() {
        val adapter = createAdapter(stars = true)

        val view = adapter.createAndBindView()

        assertThat(view.starView).isVisible()
    }

    @Test
    fun withStarsAndStarredMessage_shouldSetStarViewToSelected() {
        val adapter = createAdapter(stars = true)
        val messageListItem = createMessageListItem(isStarred = true)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.starView).isSelected()
    }

    @Test
    fun withStarsAndUnstarredMessage_shouldNotSetStarViewToSelected() {
        val adapter = createAdapter(stars = true)
        val messageListItem = createMessageListItem(isStarred = false)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.starView).isNotSelected()
    }

    @Test
    fun withoutShowContactPicture_shouldHideContactPictureView() {
        val adapter = createAdapter(showContactPicture = false)

        val view = adapter.createAndBindView()

        assertThat(view.contactPictureContainerView).isGone()
    }

    @Test
    fun withShowContactPicture_shouldShowContactPictureView() {
        val adapter = createAdapter(showContactPicture = true)

        val view = adapter.createAndBindView()

        assertThat(view.contactPictureContainerView).isVisible()
    }

    @Test
    fun withThreadCountOne_shouldHideThreadCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(threadCount = 1)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.threadCountView).isGone()
    }

    @Test
    fun withThreadCountGreaterOne_shouldShowThreadCountViewWithExpectedValue() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(threadCount = 13)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.threadCountView).isVisible()
        assertThat(view.threadCountView.textString).isEqualTo("13")
    }

    @Test
    fun withoutSenderAboveSubject_shouldShowSubjectInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = "Subject")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.firstLineView.textString).isEqualTo("Subject")
    }

    @Test
    fun withSenderAboveSubject_shouldShowDisplayNameInFirstLine() {
        val adapter = createAdapter(senderAboveSubject = true)
        val messageListItem = createMessageListItem(displayName = "Display Name")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.firstLineView.textString).isEqualTo("Display Name")
    }

    @Test
    fun withoutSenderAboveSubjectAndZeroPreviewLines_shouldShowDisplayNameInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 0)
        val messageListItem = createMessageListItem(displayName = "Display Name")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.secondLineView.textString).isEqualTo("Display Name")
    }

    @Test
    fun withoutSenderAboveSubjectAndPreviewLines_shouldShowDisplayNameAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = false, previewLines = 1)
        val messageListItem = createMessageListItem(displayName = "Display Name", previewText = "Preview")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.secondLineView.textString).isEqualTo(secondLine("Display Name", "Preview"))
    }

    @Test
    fun withSenderAboveSubjectAndZeroPreviewLines_shouldShowSubjectInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 0)
        val messageListItem = createMessageListItem(subject = "Subject")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.secondLineView.textString).isEqualTo("Subject")
    }

    @Test
    fun withSenderAboveSubjectAndPreviewLines_shouldShowSubjectAndPreviewInSecondLine() {
        val adapter = createAdapter(senderAboveSubject = true, previewLines = 1)
        val messageListItem = createMessageListItem(subject = "Subject", previewText = "Preview")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.secondLineView.textString).isEqualTo(secondLine("Subject", "Preview"))
    }

    @Test
    fun withMissingSubject_shouldDisplayNoSubjectIndicator() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = null)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.firstLineView.textString).isMissingSubjectText()
    }

    @Test
    fun withEmptySubject_shouldDisplayNoSubjectIndicator() {
        val adapter = createAdapter(senderAboveSubject = false)
        val messageListItem = createMessageListItem(subject = "")

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.firstLineView.textString).isMissingSubjectText()
    }

    @Test
    fun withoutAttachments_shouldHideAttachmentCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(hasAttachments = false)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.attachmentCountView).isGone()
    }

    @Test
    fun withAttachments_shouldShowAttachmentCountView() {
        val adapter = createAdapter()
        val messageListItem = createMessageListItem(hasAttachments = true)

        val view = adapter.createAndBindView(messageListItem)

        assertThat(view.attachmentCountView).isVisible()
    }

    @Test
    fun withoutSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(subject = FONT_DEFAULT),
            senderAboveSubject = false,
        )

        val view = adapter.createAndBindView()

        assertThat(view.firstLineView.textSize).isEqualTo(FIRST_LINE_DEFAULT_FONT_SIZE)
    }

    @Test
    fun withoutSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(subject = LARGE),
            senderAboveSubject = false,
        )

        val view = adapter.createAndBindView()

        assertThat(view.firstLineView.textSize).isEqualTo(22f)
    }

    @Test
    fun withSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(sender = FONT_DEFAULT),
            senderAboveSubject = true,
        )

        val view = adapter.createAndBindView()

        assertThat(view.firstLineView.textSize).isEqualTo(FIRST_LINE_DEFAULT_FONT_SIZE)
    }

    @Test
    fun withSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeOfFirstLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(sender = LARGE),
            senderAboveSubject = true,
        )

        val view = adapter.createAndBindView()

        assertThat(view.firstLineView.textSize).isEqualTo(22f)
    }

    @Test
    fun withoutSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(sender = FONT_DEFAULT),
            senderAboveSubject = false,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.text).firstAbsoluteSizeSpanValue().isNull()
    }

    @Test
    fun withoutSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(sender = LARGE),
            senderAboveSubject = false,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.text).firstAbsoluteSizeSpanValue().isEqualTo(22)
    }

    @Test
    fun withSenderAboveSubjectAndDefaultFontSize_shouldNotSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(subject = FONT_DEFAULT),
            senderAboveSubject = true,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.text).firstAbsoluteSizeSpanValue().isNull()
    }

    @Test
    fun withSenderAboveSubjectAndNonDefaultFontSize_shouldSetTextSizeSpanInSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(subject = LARGE),
            senderAboveSubject = true,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.text).firstAbsoluteSizeSpanValue().isEqualTo(22)
    }

    @Test
    fun dateWithDefaultFontSize_shouldNotSetTextSizeOfDateView() {
        val adapter = createAdapter(fontSizes = createFontSizes(date = FONT_DEFAULT))

        val view = adapter.createAndBindView()

        assertThat(view.dateView.textSize).isEqualTo(DATE_DEFAULT_FONT_SIZE)
    }

    @Test
    fun dateWithNonDefaultFontSize_shouldSetTextSizeOfDateView() {
        val adapter = createAdapter(fontSizes = createFontSizes(date = LARGE))

        val view = adapter.createAndBindView()

        assertThat(view.dateView.textSize).isEqualTo(22f)
    }

    @Test
    fun previewWithDefaultFontSize_shouldNotSetTextSizeOfSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(preview = FONT_DEFAULT),
            previewLines = 1,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.textSize).isEqualTo(SECOND_LINE_DEFAULT_FONT_SIZE)
    }

    @Test
    fun previewWithNonDefaultFontSize_shouldSetTextSizeOfSecondLineView() {
        val adapter = createAdapter(
            fontSizes = createFontSizes(preview = LARGE),
            previewLines = 1,
        )

        val view = adapter.createAndBindView()

        assertThat(view.secondLineView.textSize).isEqualTo(22f)
    }

    fun createFontSizes(
        subject: Int = FONT_DEFAULT,
        sender: Int = FONT_DEFAULT,
        preview: Int = FONT_DEFAULT,
        date: Int = FONT_DEFAULT,
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
        showAccountIndicator: Boolean = false,
        density: UiDensity = UiDensity.Default,
        dateTimeFormat: MessageListDateTimeFormat = DATE_TIME_FORMAT,
        keypadNavigation: () -> Boolean = { false },
    ): MessageListAdapter {
        val appearance = MessageListAppearance(
            fontSizes,
            previewLines,
            stars,
            senderAboveSubject,
            showContactPicture,
            showingThreadedList,
            backGroundAsReadIndicator,
            showAccountIndicator,
            density,
            dateTimeFormat = dateTimeFormat,
        )

        @OptIn(ExperimentalTime::class)
        return MessageListAdapter(
            theme = context.theme,
            res = context.resources,
            layoutInflater = LayoutInflater.from(context),
            contactsPictureLoader = contactsPictureLoader,
            listItemListener = listItemListener,
            appearance = { appearance.copy(isKeypadNavigationEnabled = keypadNavigation()) },
            themeProvider = FakeThemeProvider(),
            featureFlagProvider = FakeFeatureFlagProvider(),
            avatarMonogramCreator = mock(),
            contactRepository = mock(),
            formatDate = { "12:34" },
        )
    }

    fun createMessageListItem(
        account: LegacyAccount = createLegacyAccount(),
        subject: String? = "irrelevant",
        threadCount: Int = 0,
        messageDate: Long = 0L,
        internalDate: Long = 0L,
        displayName: CharSequence = "irrelevant",
        displayAddress: Address? = Address.parse("irrelevant@domain.example").first(),
        displayDateTime: String = "12:34",
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
        threadRoot: Long = 0L,
    ): MessageListItem {
        return MessageListItem(
            account,
            subject,
            threadCount,
            messageDate,
            internalDate,
            displayName,
            displayAddress,
            displayDateTime,
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
            threadRoot,
            contactColor = -1,
        )
    }

    private fun createLegacyAccount(): LegacyAccount {
        return LegacyAccount(
            id = SOME_ACCOUNT_ID,
            name = "irrelevant",
            email = "irrelevant@example.com",
            profile = createProfile(),
            incomingServerSettings = createServerSettings(),
            outgoingServerSettings = createServerSettings(),
            identities = listOf(
                Identity(
                    description = null,
                    name = "irrelevant",
                    email = "irrelevant@example.com",
                ),
            ),
        )
    }

    private fun createProfile(): ProfileDto {
        return ProfileDto(
            id = SOME_ACCOUNT_ID,
            name = "irrelevant",
            color = 0xFF00FF,
            avatar = AvatarDto(
                accountId = SOME_ACCOUNT_ID,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "ab",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        )
    }

    private fun createServerSettings(): ServerSettings {
        return ServerSettings(
            type = "imap",
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "irrelevant",
            password = "irrelevant",
            clientCertificateAlias = null,
        )
    }

    fun MessageListAdapter.createAndBindView(item: MessageListItem = createMessageListItem()): View {
        viewItems = listOf(MessageListViewItem.Message(item))
        val holder = onCreateViewHolder(LinearLayout(context), 0)
        onBindViewHolder(holder, 0)
        return holder.itemView
    }

    fun secondLine(senderOrSubject: String, preview: String) = "$senderOrSubject – $preview"

    val View.accountIndicatorView: View get() = findViewById(R.id.account_color_chip)
    val View.starView: View get() = findViewById(R.id.star)
    val View.contactPictureContainerView: View get() = findViewById(R.id.contact_picture_click_area)
    val View.threadCountView: MaterialTextView get() = findViewById(R.id.thread_count)
    val View.firstLineView: MaterialTextView get() = findViewById(R.id.subject)
    val View.secondLineView: MaterialTextView get() = findViewById(R.id.preview)
    val View.attachmentCountView: View get() = findViewById(R.id.attachment)
    val View.dateView: MaterialTextView get() = findViewById(R.id.date)

    private fun Assert<View>.isVisible() = given { actual ->
        if (!actual.isVisible) {
            expected("View to be visible ($actual)")
        }
    }

    private fun Assert<View>.isGone() = given { actual ->
        if (!actual.isGone) {
            expected("View to be gone ($actual)")
        }
    }

    private fun Assert<View>.isSelected() = given { actual ->
        if (!actual.isSelected) {
            expected("View to be selected ($actual)")
        }
    }

    private fun Assert<View>.isNotSelected() = given { actual ->
        if (actual.isSelected) {
            expected("View to not be selected ($actual)")
        }
    }

    private fun Assert<CharSequence>.isMissingSubjectText() {
        isEqualTo(context.getString(R.string.general_no_subject))
    }

    private fun Assert<CharSequence>.firstAbsoluteSizeSpanValue() = transform { text ->
        val spans = (text as Spannable).getSpans(0, text.length, AbsoluteSizeSpan::class.java)
        spans.firstOrNull()?.size
    }

    private val MaterialTextView.textString: String
        get() = text.toString()

    private class FakeThemeProvider : FeatureThemeProvider {
        @Composable
        override fun WithTheme(content: @Composable (() -> Unit)) {
            content()
        }

        @Composable
        override fun WithTheme(
            darkTheme: Boolean,
            content: @Composable (() -> Unit),
        ) {
            content()
        }
    }

    private class FakeFeatureFlagProvider : FeatureFlagProvider {
        // Disabled as the test is primarily concerned with the XML based UI
        override fun provide(key: FeatureFlagKey): FeatureFlagResult = FeatureFlagResult.Disabled
    }
}
