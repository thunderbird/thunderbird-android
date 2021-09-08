package com.fsck.k9.ui.messageview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
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
import com.fsck.k9.view.MessageWebView;
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

    private static final float SWIPE_START_THRESHOLD = 0.05f; // fraction of view width
    private static final float SWIPE_EXECUTE_THRESHOLD = 0.5f; // fraction of view width
    private static final float SWIPE_TRANSLATE_DELAY_FACTOR = 0.5f; // mSec per pixel
    private static final float SWIPE_TRANSLATE_ALPHA = 0.5f; // i.e. 50% transparency

    private SwipeCatcher swipeCatcher;
    private SwipeMode swipeMode;
    private MotionEvent swipeOrigin;
    private float swipePriorDeltaX;

    private MessageWebView messageWebView;

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

    private MessageWebView swipeGetWebView() {
        if (messageWebView == null) {
            messageWebView = findViewById(R.id.message_content);
        }
        return messageWebView;
    }

    private void swipeAnimateTo(float newDeltaX) {
        setAlpha(newDeltaX != 0 ? SWIPE_TRANSLATE_ALPHA : 1f);
        TranslateAnimation animation = new TranslateAnimation(swipePriorDeltaX, newDeltaX, 0, 0);
        animation.setDuration((long) (Math.abs(newDeltaX - swipePriorDeltaX) * SWIPE_TRANSLATE_DELAY_FACTOR));
        animation.setFillAfter(true);
        startAnimation(animation);
        swipePriorDeltaX = newDeltaX;
    }

    private boolean swipeWebViewCanScroll(SwipeMode swipeMode) {
        switch (swipeMode) {
            case SWIPING_LEFT: {
                return swipeGetWebView().canScrollHorizontally(1);
            }
            case SWIPING_RIGHT: {
                return swipeGetWebView().canScrollHorizontally(-1);
            }
        }
        return false;
    }

    private boolean swipeInsideWebView() {
        Rect webViewRect = new Rect();
        swipeGetWebView().getHitRect(webViewRect);
        int[] webViewOrigin = new int[2];
        swipeGetWebView().getLocationOnScreen(webViewOrigin);
        webViewRect.offset(webViewOrigin[0], webViewOrigin[1]);
        return webViewRect.contains((int) swipeOrigin.getRawX(), (int) swipeOrigin.getRawY());
    }

    private SwipeMode swipeGetSwipeMode(MotionEvent swipeDestination) {
        float diffX = Math.abs(swipeDestination.getX() - swipeOrigin.getX());
        float diffY = Math.abs(swipeDestination.getY() - swipeOrigin.getY());
        if ((diffX > diffY) && (diffX > (getWidth() * SWIPE_START_THRESHOLD))) {
            return swipeDestination.getX() < swipeOrigin.getX() ? SwipeMode.SWIPING_LEFT : SwipeMode.SWIPING_RIGHT;
        }
        return SwipeMode.DISABLED;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                if (swipeMode != SwipeMode.DISABLED) {
                    float newDeltaX = event.getX() - swipeOrigin.getX();
                    SwipeMode newMode = newDeltaX < 0 ? SwipeMode.SWIPING_LEFT : SwipeMode.SWIPING_RIGHT;
                    if (newMode == swipeMode) {
                        swipeAnimateTo(newDeltaX);
                        float viewWidth = getWidth();
                        if (Math.abs(newDeltaX) > (viewWidth * SWIPE_EXECUTE_THRESHOLD)) {
                            if (swipeCatcher != null && swipeCatcher.canSwipe(swipeMode)) {
                                swipeAnimateTo(swipeMode == SwipeMode.SWIPING_LEFT ? -viewWidth : viewWidth);
                                performHapticFeedback(HapticFeedbackConstants.GESTURE_END);
                                swipeCatcher.doSwipe(swipeMode);
                            }
                            swipeMode = SwipeMode.DISABLED;
                        }
                    } else {
                        swipeAnimateTo(0);
                        swipeMode = SwipeMode.DISABLED;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (swipeMode != SwipeMode.DISABLED) {
                    swipeAnimateTo(0);
                    swipeMode = SwipeMode.DISABLED;
                }
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
                    swipeOrigin = MotionEvent.obtainNoHistory(event);
                    swipeMode = SwipeMode.DISABLED;
                    swipePriorDeltaX = 0;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (swipeMode == SwipeMode.DISABLED) {
                        SwipeMode newMode = swipeGetSwipeMode(event);
                        if ((newMode != SwipeMode.DISABLED) && swipeCatcher.canSwipe(newMode) && (!swipeInsideWebView() || !swipeWebViewCanScroll(newMode))) {
                            swipeMode = newMode;
                            performHapticFeedback(HapticFeedbackConstants.GESTURE_START);
                        }
                    }
                    if (swipeMode != SwipeMode.DISABLED) {
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (swipeMode != SwipeMode.DISABLED) {
                        return true;
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event);
    }
}
