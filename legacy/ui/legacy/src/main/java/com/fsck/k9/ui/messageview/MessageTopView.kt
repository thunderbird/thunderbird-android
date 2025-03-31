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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.mail.toEmailAddressOrNull
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.account.ShowPictures
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messageview.MessageContainerView.OnRenderingFinishedListener
import com.fsck.k9.view.MessageHeader
import com.fsck.k9.view.ThemeUtils
import com.fsck.k9.view.ToolableViewAnimator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageTopView(
    context: Context,
    attrs: AttributeSet?,
) : LinearLayout(context, attrs), KoinComponent {

    private val contactRepository: ContactRepository by inject()

    private lateinit var layoutInflater: LayoutInflater

    private lateinit var viewAnimator: ToolableViewAnimator
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: MaterialTextView

    lateinit var messageHeaderView: MessageHeader

    private lateinit var containerView: ViewGroup
    private lateinit var downloadRemainderButton: MaterialButton
    private lateinit var attachmentCallback: AttachmentViewCallback
    private lateinit var extraHeaderContainer: View
    private lateinit var showPicturesButton: MaterialButton

    private var isShowingProgress = false
    private var showPicturesButtonClicked = false

    private var showAccountChip = false

    private var messageCryptoPresenter: MessageCryptoPresenter? = null

    public override fun onFinishInflate() {
        super.onFinishInflate()

        messageHeaderView = findViewById(R.id.header_container)
        layoutInflater = LayoutInflater.from(context)

        viewAnimator = findViewById(R.id.message_layout_animator)
        progressBar = findViewById(R.id.message_progress)
        progressText = findViewById(R.id.message_progress_text)

        downloadRemainderButton = findViewById(R.id.download_remainder)
        downloadRemainderButton.visibility = GONE

        extraHeaderContainer = findViewById(R.id.extra_header_container)
        showPicturesButton = findViewById(R.id.show_pictures)
        setShowPicturesButtonListener()

        containerView = findViewById(R.id.message_container)

        hideHeaderView()
    }

    fun setShowAccountChip(showAccountChip: Boolean) {
        this.showAccountChip = showAccountChip
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
            object : OnRenderingFinishedListener {
                override fun onLoadFinished() {
                    displayViewOnLoadFinished(true)
                }
            },
            loadPictures,
            hideUnsignedTextDivider,
            attachmentCallback,
        )

        if (view.hasHiddenExternalImages && !showPicturesButtonClicked) {
            showShowPicturesButton()
        }
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
        messageHeaderView.populate(message, account, showStar, showAccountChip)
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
