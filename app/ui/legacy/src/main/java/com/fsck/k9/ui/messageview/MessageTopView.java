package com.fsck.k9.ui.messageview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.ui.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.ui.messageview.MessageContainerView.OnRenderingFinishedListener;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.ThemeUtils;
import com.fsck.k9.view.ToolableViewAnimator;
import org.openintents.openpgp.OpenPgpError;


public class MessageTopView extends LinearLayout {

    public static final int PROGRESS_MAX = 1000;
    public static final int PROGRESS_MAX_WITH_MARGIN = 950;
    public static final int PROGRESS_STEP_DURATION = 180;


    private ToolableViewAnimator viewAnimator;
    private ProgressBar progressBar;
    private TextView progressText;

    private MessageHeader mHeaderContainer;
    private LayoutInflater mInflater;
    private ViewGroup containerView;
    private Button mDownloadRemainder;
    private AttachmentViewCallback attachmentCallback;
    private Button showPicturesButton;
    private boolean isShowingProgress;
    private boolean showPicturesButtonClicked;

    private MessageCryptoPresenter messageCryptoPresenter;

    private static final int SWIPE_START_THRESHOLD = 10;
    private static final int SWIPE_ANIMATION_DELTA = 500;
    private static final int SWIPE_ANIMATION_DURATION = 500;
    private SwipeCatcher swipeCatcher;
    private boolean swipeCatching;
    private boolean swipeFeedbackVisible;
    private float swipeOriginX;
    private float swipeOriginY;
    private boolean swipeToLeft;

    public MessageTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mHeaderContainer = findViewById(R.id.header_container);
        mInflater = LayoutInflater.from(getContext());

        viewAnimator = findViewById(R.id.message_layout_animator);
        progressBar = findViewById(R.id.message_progress);
        progressText = findViewById(R.id.message_progress_text);

        mDownloadRemainder = findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);

        showPicturesButton = findViewById(R.id.show_pictures);
        setShowPicturesButtonListener();

        containerView = findViewById(R.id.message_container);

        hideHeaderView();
    }

    private void setShowPicturesButtonListener() {
        showPicturesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPicturesInAllContainerViews();
                showPicturesButtonClicked = true;
            }
        });
    }

    private void showPicturesInAllContainerViews() {
        View messageContainerViewCandidate = containerView.getChildAt(0);
        if (messageContainerViewCandidate instanceof MessageContainerView) {
            ((MessageContainerView) messageContainerViewCandidate).showPictures();
        }
        hideShowPicturesButton();
    }

    private void resetAndPrepareMessageView(MessageViewInfo messageViewInfo) {
        mDownloadRemainder.setVisibility(View.GONE);
        containerView.removeAllViews();
        setShowDownloadButton(messageViewInfo);
    }

    public void showMessage(Account account, MessageViewInfo messageViewInfo) {
        resetAndPrepareMessageView(messageViewInfo);

        ShowPictures showPicturesSetting = account.getShowPictures();
        boolean loadPictures = shouldAutomaticallyLoadPictures(showPicturesSetting, messageViewInfo.message) ||
                showPicturesButtonClicked;

        MessageContainerView view = (MessageContainerView) mInflater.inflate(R.layout.message_container,
                containerView, false);
        containerView.addView(view);

        boolean hideUnsignedTextDivider = account.isOpenPgpHideSignOnly();
        view.displayMessageViewContainer(messageViewInfo, new OnRenderingFinishedListener() {
            @Override
            public void onLoadFinished() {
                displayViewOnLoadFinished(true);
            }
        }, loadPictures, hideUnsignedTextDivider, attachmentCallback);

        if (view.hasHiddenExternalImages() && !showPicturesButtonClicked) {
            showShowPicturesButton();
        }
    }

    public void showMessageEncryptedButIncomplete(MessageViewInfo messageViewInfo, Drawable providerIcon) {
        resetAndPrepareMessageView(messageViewInfo);
        View view = mInflater.inflate(R.layout.message_content_crypto_incomplete, containerView, false);
        setCryptoProviderIcon(providerIcon, view);

        containerView.addView(view);
        displayViewOnLoadFinished(false);
    }

    public void showMessageCryptoErrorView(MessageViewInfo messageViewInfo, Drawable providerIcon) {
        resetAndPrepareMessageView(messageViewInfo);
        View view = mInflater.inflate(R.layout.message_content_crypto_error, containerView, false);
        setCryptoProviderIcon(providerIcon, view);

        TextView cryptoErrorText = view.findViewById(R.id.crypto_error_text);
        OpenPgpError openPgpError = messageViewInfo.cryptoResultAnnotation.getOpenPgpError();
        if (openPgpError != null) {
            String errorText = openPgpError.getMessage();
            cryptoErrorText.setText(errorText);
        }

        containerView.addView(view);
        displayViewOnLoadFinished(false);
    }

    public void showMessageCryptoCancelledView(MessageViewInfo messageViewInfo, Drawable providerIcon) {
        resetAndPrepareMessageView(messageViewInfo);
        View view = mInflater.inflate(R.layout.message_content_crypto_cancelled, containerView, false);
        setCryptoProviderIcon(providerIcon, view);

        view.findViewById(R.id.crypto_cancelled_retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                messageCryptoPresenter.onClickRetryCryptoOperation();
            }
        });

        containerView.addView(view);
        displayViewOnLoadFinished(false);
    }

    public void showCryptoProviderNotConfigured(final MessageViewInfo messageViewInfo) {
        resetAndPrepareMessageView(messageViewInfo);
        View view = mInflater.inflate(R.layout.message_content_crypto_no_provider, containerView, false);

        view.findViewById(R.id.crypto_settings).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                messageCryptoPresenter.onClickConfigureProvider();
            }
        });

        containerView.addView(view);
        displayViewOnLoadFinished(false);
    }

    private void setCryptoProviderIcon(Drawable openPgpApiProviderIcon, View view) {
        ImageView cryptoProviderIcon = view.findViewById(R.id.crypto_error_icon);
        if (openPgpApiProviderIcon != null) {
            cryptoProviderIcon.setImageDrawable(openPgpApiProviderIcon);
        } else {
            cryptoProviderIcon.setImageResource(R.drawable.status_lock_error);
            cryptoProviderIcon.setColorFilter(ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_red));
        }
    }

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setHeaders(Message message, Account account, boolean showStar) {
        mHeaderContainer.populate(message, account, showStar);
        mHeaderContainer.setVisibility(View.VISIBLE);
    }

    public void setSubject(@NonNull String subject) {
        mHeaderContainer.setSubject(subject);
    }

    public void setOnToggleFlagClickListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mHeaderContainer.setOnMenuItemClickListener(listener);
    }

    private void hideHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public void setOnDownloadButtonClickListener(OnClickListener listener) {
        mDownloadRemainder.setOnClickListener(listener);
    }

    public void setAttachmentCallback(AttachmentViewCallback callback) {
        attachmentCallback = callback;
    }

    public void setMessageCryptoPresenter(MessageCryptoPresenter messageCryptoPresenter) {
        this.messageCryptoPresenter = messageCryptoPresenter;
        mHeaderContainer.setOnCryptoClickListener(messageCryptoPresenter);
    }

    public void enableDownloadButton() {
        mDownloadRemainder.setEnabled(true);
    }

    public void disableDownloadButton() {
        mDownloadRemainder.setEnabled(false);
    }

    private void setShowDownloadButton(MessageViewInfo messageViewInfo) {
        if (messageViewInfo.isMessageIncomplete) {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        } else {
            mDownloadRemainder.setVisibility(View.GONE);
        }
    }

    private void showShowPicturesButton() {
        showPicturesButton.setVisibility(View.VISIBLE);
    }

    private void hideShowPicturesButton() {
        showPicturesButton.setVisibility(View.GONE);
    }

    private boolean shouldAutomaticallyLoadPictures(ShowPictures showPicturesSetting, Message message) {
        return showPicturesSetting == ShowPictures.ALWAYS || shouldShowPicturesFromSender(showPicturesSetting, message);
    }

    private boolean shouldShowPicturesFromSender(ShowPictures showPicturesSetting, Message message) {
        if (showPicturesSetting != ShowPictures.ONLY_FROM_CONTACTS) {
            return false;
        }

        String senderEmailAddress = getSenderEmailAddress(message);
        if (senderEmailAddress == null) {
            return false;
        }

        Contacts contacts = Contacts.getInstance(getContext());
        return contacts.isInContacts(senderEmailAddress);
    }

    private String getSenderEmailAddress(Message message) {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) {
            return null;
        }

        return from[0].getAddress();
    }

    public void displayViewOnLoadFinished(boolean finishProgressBar) {
        if (!finishProgressBar || !isShowingProgress) {
            viewAnimator.setDisplayedChild(2);
            return;
        }

        ObjectAnimator animator = ObjectAnimator.ofInt(
                progressBar, "progress", progressBar.getProgress(), PROGRESS_MAX);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                viewAnimator.setDisplayedChild(2);
            }
        });
        animator.setDuration(PROGRESS_STEP_DURATION);
        animator.start();
    }

    public void setToLoadingState() {
        viewAnimator.setDisplayedChild(0);
        progressBar.setProgress(0);
        isShowingProgress = false;
    }

    public void setLoadingProgress(int progress, int max) {
        if (!isShowingProgress) {
            viewAnimator.setDisplayedChild(1);
            isShowingProgress = true;
            return;
        }

        int newPosition = (int) (progress / (float) max * PROGRESS_MAX_WITH_MARGIN);
        int currentPosition = progressBar.getProgress();
        if (newPosition > currentPosition) {
            ObjectAnimator.ofInt(progressBar, "progress", currentPosition, newPosition)
                    .setDuration(PROGRESS_STEP_DURATION).start();
        } else {
            progressBar.setProgress(newPosition);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.showPicturesButtonClicked = showPicturesButtonClicked;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        showPicturesButtonClicked = savedState.showPicturesButtonClicked;
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        View messageContainerViewCandidate = containerView.getChildAt(0);
        if (messageContainerViewCandidate instanceof MessageContainerView) {
            ((MessageContainerView) messageContainerViewCandidate).refreshAttachmentThumbnail(attachment);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean showPicturesButtonClicked;

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.showPicturesButtonClicked = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.showPicturesButtonClicked) ? 1 : 0);
        }
    }

    public void setSwipeCatcher(SwipeCatcher catcher) {
        swipeCatcher = catcher;
    }

    private void showSwipeFeedback() {
        if (!swipeFeedbackVisible) {
            float deltaX = swipeToLeft ? -SWIPE_ANIMATION_DELTA : SWIPE_ANIMATION_DELTA;
            TranslateAnimation swipeAnimation = new TranslateAnimation(0, deltaX, 0, 0);
            swipeAnimation.setDuration(SWIPE_ANIMATION_DURATION);
            swipeAnimation.setFillAfter(true);
            startAnimation(swipeAnimation);
            performHapticFeedback(HapticFeedbackConstants.GESTURE_START);
            swipeFeedbackVisible = true;
        }
    }

    private void hideSwipeFeedback() {
        if (swipeFeedbackVisible) {
            TranslateAnimation cancelAnimation = new TranslateAnimation(0, 0, 0, 0);
            cancelAnimation.setDuration(SWIPE_ANIMATION_DURATION);
            cancelAnimation.setFillAfter(true);
            startAnimation(cancelAnimation);
            performHapticFeedback(HapticFeedbackConstants.GESTURE_END);
            swipeFeedbackVisible = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                boolean thisSwipeToLeft = (event.getX() < swipeOriginX);
                if (thisSwipeToLeft != swipeToLeft) {
                    // swipe direction was reversed
                    hideSwipeFeedback();
                    swipeCatching = false;
                    swipeToLeft = thisSwipeToLeft;
                }
                if (swipeCatching) {
                    showSwipeFeedback();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (swipeCatching && (swipeCatcher != null)) {
                    swipeCatcher.onSwipe(swipeToLeft);
                }
                // fall through ...
            }
            case MotionEvent.ACTION_CANCEL: {
                hideSwipeFeedback();
                swipeCatching = false;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (swipeCatcher != null) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    swipeCatching = false;
                    swipeFeedbackVisible = false;
                    swipeOriginX = event.getX();
                    swipeOriginY = event.getY();
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (swipeCatching) {
                        return true;
                    }
                    float swipeWidth = Math.abs(event.getX() - swipeOriginX);
                    float swipeHeight = Math.abs(event.getY() - swipeOriginY);
                    if ((swipeWidth > swipeHeight) && (swipeWidth > SWIPE_START_THRESHOLD)) {
                        swipeCatching = true;
                        swipeToLeft = event.getX() < swipeOriginX;
                        return true;
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event);
    }
}
