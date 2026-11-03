package com.fsck.k9.ui.messageview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.view.doOnLayout
import app.k9mail.core.android.common.contact.ContactRepository
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.SizeFormatter
import com.fsck.k9.ui.messageview.MessageContainerView.OnRenderingFinishedListener
import com.fsck.k9.view.MessageHeader
import com.fsck.k9.view.MessageWebView
import com.fsck.k9.view.NonLockingScrollView
import com.fsck.k9.view.ThemeUtils
import com.fsck.k9.view.ToolableViewAnimator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.mail.toEmailAddressOrNull
import net.thunderbird.core.preference.BodyContentType
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.ui.animation.manager.AnimationManager
import net.thunderbird.feature.mail.message.reader.api.ui.MessageReaderViewContract
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageTopView(
    context: Context,
    attrs: AttributeSet?,
) : LinearLayout(context, attrs), KoinComponent {

    private val contactRepository: ContactRepository by inject()
    private val visualSettingsPrefManager: DisplayVisualSettingsPreferenceManager by inject()
    private val animationManager: AnimationManager by inject()

    private lateinit var layoutInflater: LayoutInflater

    private lateinit var viewAnimator: ToolableViewAnimator
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: MaterialTextView

    lateinit var messageHeaderView: MessageHeader

    private lateinit var containerView: ViewGroup
    private lateinit var downloadRemainderButton: MaterialButton
    private lateinit var attachmentCallback: AttachmentViewCallback
    private lateinit var messageReaderViewModel: MessageReaderViewContract.ViewModel<Part>
    private lateinit var extraHeaderContainer: View
    private lateinit var showPicturesButton: MaterialButton

    private var isShowingProgress = false
    private var showPicturesButtonClicked = false
    var renderPlainFormat = false

    private var showAccountIndicator = false

    private var messageCryptoPresenter: MessageCryptoPresenter? = null
    private var keypadNavigation: MessageReaderKeypadNavigation? = null
    private val initialKeypadFocus by lazy {
        MessageReaderInitialFocus(
            scrollView = findViewById(R.id.message_scrollview),
            body = { findViewById<MessageWebView>(R.id.message_content) },
            isReaderReady = { isKeypadReaderActive() && isShown && hasWindowFocus() },
            defaultFocusContainer = { rootView.findViewById(R.id.toolbar) },
        )
    }
    var isKeypadReaderActive: () -> Boolean = { false }

    fun requestInitialKeypadFocus() {
        post { doOnLayout { initialKeypadFocus.tryFocusBody() } }
    }

    fun setKeypadNavigationEnabled(enabled: Boolean) {
        findViewById<NonLockingScrollView>(R.id.message_scrollview).setKeypadNavigationEnabled(enabled)
        keypadNavigation?.reset()
    }

    fun dispatchKeypadNavigation(event: KeyEvent): Boolean {
        if (!isShown || !hasWindowFocus() || rootView.findFocus()?.onCheckIsTextEditor() == true) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) onKeypadUserInteraction()
        val navigation = keypadNavigation ?: MessageReaderKeypadNavigation(
            scrollView = findViewById(R.id.message_scrollview),
            body = { findViewById<MessageWebView>(R.id.message_content)?.takeIf { it.isShown } },
            reply = { findViewById(R.id.menu_primary_action) },
            attachments = { findViewById(R.id.view_all_attachments) },
        ).also { keypadNavigation = it }
        return navigation.dispatchKeyEvent(event)
    }

    fun onKeypadUserInteraction() {
        if (isKeypadReaderActive() && isShown && hasWindowFocus()) initialKeypadFocus.onUserInteraction()
    }

    fun resetKeypadNavigation() {
        keypadNavigation?.reset()
    }

    override fun onDetachedFromWindow() {
        resetKeypadNavigation()
        super.onDetachedFromWindow()
    }

    override fun dispatchWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) resetKeypadNavigation()
        super.dispatchWindowFocusChanged(hasFocus)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) onKeypadUserInteraction()
        return super.dispatchTouchEvent(event)
    }

    public override fun onFinishInflate() {
        super.onFinishInflate()

        messageHeaderView = findViewById(R.id.header_container)
        layoutInflater = LayoutInflater.from(context)

        viewAnimator = findViewById(R.id.message_layout_animator)
        if (!animationManager.shouldShowAnimations()) {
            viewAnimator.inAnimation = null
            viewAnimator.outAnimation = null
        }
        progressBar = findViewById(R.id.message_progress)
        progressText = findViewById(R.id.message_progress_text)

        downloadRemainderButton = findViewById(R.id.download_remainder)
        downloadRemainderButton.visibility = GONE

        extraHeaderContainer = findViewById(R.id.extra_header_container)
        showPicturesButton = findViewById(R.id.show_pictures)
        setShowPicturesButtonListener()

        containerView = findViewById(R.id.message_container)

        renderPlainFormat = visualSettingsPrefManager.getConfig().bodyContentType == BodyContentType.TEXT_PLAIN

        hideHeaderView()
    }

    fun setShowAccountIndicator(showAccountIndicator: Boolean) {
        this.showAccountIndicator = showAccountIndicator
    }

    private fun setShowPicturesButtonListener() {
        showPicturesButton.setOnClickListener {
            showPicturesInAllContainerViews()
            showPicturesButtonClicked = true
        }
    }

    private fun showPicturesInAllContainerViews() {
        val messageContainerViewCandidate = containerView.getChildAt(0)
        if (messageContainerViewCandidate is MessageContainerView) {
            messageContainerViewCandidate.showPictures()
        }
        hideShowPicturesButton()
    }

    private fun resetAndPrepareMessageView(messageViewInfo: MessageViewInfo) {
        downloadRemainderButton.visibility = GONE
        containerView.removeAllViews()
        setShowDownloadButton(messageViewInfo)
    }

    fun showMessage(account: LegacyAccount, messageViewInfo: MessageViewInfo) {
        resetAndPrepareMessageView(messageViewInfo)

        val showPicturesSetting = account.showPictures
        val loadPictures = shouldAutomaticallyLoadPictures(showPicturesSetting, messageViewInfo.message) ||
            showPicturesButtonClicked

        val view = layoutInflater.inflate(
            R.layout.message_container,
            containerView,
            false,
        ) as MessageContainerView
        containerView.addView(view)

        val hideUnsignedTextDivider = account.isOpenPgpHideSignOnly
        view.displayMessageViewContainer(
            messageViewInfo,
            renderPlainFormat,
            object : OnRenderingFinishedListener {
                override fun onLoadFinished() {
                    initialKeypadFocus.onBodyRendered()
                    displayViewOnLoadFinished(true)
                }
            },
            loadPictures,
            hideUnsignedTextDivider,
            attachmentCallback,
            messageReaderViewModel,
        )

        if (view.hasHiddenExternalImages && !showPicturesButtonClicked) {
            showShowPicturesButton()
        } else {
            hideShowPicturesButton()
        }

        updateAttachmentSummary(messageViewInfo)
    }

    private fun updateAttachmentSummary(messageViewInfo: MessageViewInfo) {
        val nonInlineAttachments = messageViewInfo.attachments
            ?.filter { !it.inlineAttachment }
            .orEmpty()

        val extraNonInlineAttachments = messageViewInfo.extraAttachments
            ?.filter { !it.inlineAttachment }
            .orEmpty()

        val allAttachments = nonInlineAttachments + extraNonInlineAttachments

        if (allAttachments.isEmpty()) {
            messageHeaderView.hideAttachmentSummary()
            return
        }

        val count = allAttachments.size
        val totalSize = allAttachments.sumOf { it.size.coerceAtLeast(0) }
        val sizeFormatter = SizeFormatter(context.resources)
        val sizeText = sizeFormatter.formatSize(totalSize)

        val summaryText: String
        val viewButtonText: String

        if (count == 1) {
            val fileName = allAttachments[0].displayName
            summaryText = context.getString(R.string.message_view_single_attachment_summary, fileName, sizeText)
            viewButtonText = context.getString(R.string.message_view_attachments_view)
        } else {
            summaryText = context.resources.getQuantityString(
                R.plurals.message_view_attachment_summary,
                count,
                count,
                sizeText,
            )
            viewButtonText = context.getString(R.string.message_view_attachments_view_all)
        }

        messageHeaderView.setAttachmentSummary(summaryText, viewButtonText)
    }

    fun showMessageEncryptedButIncomplete(messageViewInfo: MessageViewInfo, providerIcon: Drawable?) {
        resetAndPrepareMessageView(messageViewInfo)
        val view = layoutInflater.inflate(R.layout.message_content_crypto_incomplete, containerView, false)
        setCryptoProviderIcon(providerIcon, view)
        containerView.addView(view)
        displayViewOnLoadFinished(false)
    }

    fun showMessageCryptoErrorView(messageViewInfo: MessageViewInfo, providerIcon: Drawable?) {
        resetAndPrepareMessageView(messageViewInfo)
        val view = layoutInflater.inflate(R.layout.message_content_crypto_error, containerView, false)
        setCryptoProviderIcon(providerIcon, view)
        val cryptoErrorText = view.findViewById<MaterialTextView>(R.id.crypto_error_text)
        val openPgpError = messageViewInfo.cryptoResultAnnotation.openPgpError
        if (openPgpError != null) {
            val errorText = openPgpError.message
            cryptoErrorText.text = errorText
        }

        containerView.addView(view)
        displayViewOnLoadFinished(false)
    }

    fun showMessageCryptoCancelledView(messageViewInfo: MessageViewInfo, providerIcon: Drawable?) {
        resetAndPrepareMessageView(messageViewInfo)
        val view = layoutInflater.inflate(R.layout.message_content_crypto_cancelled, containerView, false)
        setCryptoProviderIcon(providerIcon, view)

        view.findViewById<View>(R.id.crypto_cancelled_retry)
            .setOnClickListener { messageCryptoPresenter?.onClickRetryCryptoOperation() }

        containerView.addView(view)
        displayViewOnLoadFinished(false)
    }

    fun showCryptoProviderNotConfigured(messageViewInfo: MessageViewInfo) {
        resetAndPrepareMessageView(messageViewInfo)
        val view = layoutInflater.inflate(R.layout.message_content_crypto_no_provider, containerView, false)

        view.findViewById<View>(R.id.crypto_settings)
            .setOnClickListener { messageCryptoPresenter?.onClickConfigureProvider() }

        containerView.addView(view)
        displayViewOnLoadFinished(false)
    }

    private fun setCryptoProviderIcon(openPgpApiProviderIcon: Drawable?, view: View) {
        val cryptoProviderIcon = view.findViewById<ImageView>(R.id.crypto_error_icon)
        if (openPgpApiProviderIcon != null) {
            cryptoProviderIcon.setImageDrawable(openPgpApiProviderIcon)
        } else {
            cryptoProviderIcon.setImageResource(R.drawable.status_lock_error)
            cryptoProviderIcon.setColorFilter(ThemeUtils.getStyledColor(context, R.attr.openpgp_red))
        }
    }

    fun setHeaders(message: Message?, account: LegacyAccount?, showStar: Boolean) {
        messageHeaderView.populate(message, account, showStar, showAccountIndicator)
        messageHeaderView.visibility = VISIBLE
    }

    fun setSubject(subject: String) {
        messageHeaderView.setSubject(subject)
    }

    fun setOnToggleFlagClickListener(listener: OnClickListener?) {
        messageHeaderView.setOnFlagListener(listener)
    }

    fun setMessageHeaderClickListener(listener: MessageHeaderClickListener?) {
        messageHeaderView.setMessageHeaderClickListener(listener)
    }

    private fun hideHeaderView() {
        messageHeaderView.visibility = GONE
    }

    fun setOnDownloadButtonClickListener(listener: OnClickListener?) {
        downloadRemainderButton.setOnClickListener(listener)
    }

    fun setAttachmentCallback(callback: AttachmentViewCallback) {
        attachmentCallback = callback
    }

    fun setMessageReaderViewModel(viewModel: MessageReaderViewContract.ViewModel<Part>) {
        this.messageReaderViewModel = viewModel
    }

    fun setMessageCryptoPresenter(messageCryptoPresenter: MessageCryptoPresenter?) {
        this.messageCryptoPresenter = messageCryptoPresenter
    }

    fun enableDownloadButton() {
        downloadRemainderButton.isEnabled = true
    }

    fun disableDownloadButton() {
        downloadRemainderButton.isEnabled = false
    }

    private fun setShowDownloadButton(messageViewInfo: MessageViewInfo) {
        if (messageViewInfo.isMessageIncomplete) {
            downloadRemainderButton.isEnabled = true
            downloadRemainderButton.visibility = VISIBLE
        } else {
            downloadRemainderButton.visibility = GONE
        }
    }

    private fun showShowPicturesButton() {
        extraHeaderContainer.visibility = VISIBLE
    }

    private fun hideShowPicturesButton() {
        extraHeaderContainer.visibility = GONE
    }

    private fun shouldAutomaticallyLoadPictures(showPicturesSetting: ShowPictures, message: Message): Boolean {
        return showPicturesSetting === ShowPictures.ALWAYS || shouldShowPicturesFromSender(showPicturesSetting, message)
    }

    private fun shouldShowPicturesFromSender(showPicturesSetting: ShowPictures, message: Message): Boolean {
        if (showPicturesSetting !== ShowPictures.ONLY_FROM_CONTACTS) {
            return false
        }
        val senderEmailAddress = getSenderEmailAddress(message) ?: return false
        return contactRepository.hasContactFor(senderEmailAddress)
    }

    private fun getSenderEmailAddress(message: Message): EmailAddress? {
        val from = message.from
        return if (from == null || from.isEmpty()) {
            null
        } else {
            from[0].address.toEmailAddressOrNull()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun displayViewOnLoadFinished(finishProgressBar: Boolean) {
        if (!finishProgressBar || !isShowingProgress) {
            viewAnimator.displayedChild = 2
            requestInitialKeypadFocus()
            return
        }
        val animator = ObjectAnimator.ofInt(
            progressBar,
            "progress",
            progressBar.progress,
            PROGRESS_MAX,
        )
        animator.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    viewAnimator.displayedChild = 2
                    requestInitialKeypadFocus()
                }
            },
        )
        animator.duration = PROGRESS_STEP_DURATION.toLong()
        animator.start()
    }

    fun setToLoadingState() {
        viewAnimator.displayedChild = 0
        progressBar.progress = 0
        isShowingProgress = false
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun setLoadingProgress(progress: Int, max: Int) {
        if (!isShowingProgress) {
            viewAnimator.displayedChild = 1
            isShowingProgress = true
            return
        }
        val newPosition = (progress / max.toFloat() * PROGRESS_MAX_WITH_MARGIN).toInt()
        val currentPosition = progressBar.progress
        if (newPosition > currentPosition) {
            ObjectAnimator.ofInt(progressBar, "progress", currentPosition, newPosition)
                .setDuration(PROGRESS_STEP_DURATION.toLong()).start()
        } else {
            progressBar.progress = newPosition
        }
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.showPicturesButtonClicked = showPicturesButtonClicked
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        showPicturesButtonClicked = savedState.showPicturesButtonClicked
    }

    fun refreshAttachmentThumbnail(attachment: AttachmentViewInfo) {
        val messageContainerViewCandidate = containerView.getChildAt(0)
        if (messageContainerViewCandidate is MessageContainerView) {
            messageContainerViewCandidate.refreshAttachmentThumbnail(attachment)
        }
    }

    private class SavedState : BaseSavedState {
        var showPicturesButtonClicked = false

        constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            showPicturesButtonClicked = `in`.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (showPicturesButtonClicked) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val PROGRESS_MAX = 1000
        const val PROGRESS_MAX_WITH_MARGIN = 950
        const val PROGRESS_STEP_DURATION = 180
    }
}
