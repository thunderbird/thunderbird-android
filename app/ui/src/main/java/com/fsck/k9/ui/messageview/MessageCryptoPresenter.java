package com.fsck.k9.ui.messageview;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import timber.log.Timber;


@SuppressWarnings("WeakerAccess")
public class MessageCryptoPresenter implements OnCryptoClickListener {
    public static final int REQUEST_CODE_UNKNOWN_KEY = 123;
    public static final int REQUEST_CODE_SECURITY_WARNING = 124;


    // injected state
    private final MessageCryptoMvpView messageCryptoMvpView;


    // transient state
    private CryptoResultAnnotation cryptoResultAnnotation;
    private boolean reloadOnResumeWithoutRecreateFlag;


    public MessageCryptoPresenter(MessageCryptoMvpView messageCryptoMvpView) {
        this.messageCryptoMvpView = messageCryptoMvpView;
    }

    public void onResume() {
        if (reloadOnResumeWithoutRecreateFlag) {
            reloadOnResumeWithoutRecreateFlag = false;
            messageCryptoMvpView.restartMessageCryptoProcessing();
        }
    }

    public boolean maybeHandleShowMessage(MessageTopView messageView, Account account, MessageViewInfo messageViewInfo) {
        this.cryptoResultAnnotation = messageViewInfo.cryptoResultAnnotation;

        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.fromResultAnnotation(messageViewInfo.cryptoResultAnnotation);
        if (displayStatus == MessageCryptoDisplayStatus.DISABLED) {
            return false;
        }

        messageView.getMessageHeaderView().setCryptoStatus(displayStatus);

        switch (displayStatus) {
            case CANCELLED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account.getOpenPgpProvider());
                messageView.showMessageCryptoCancelledView(messageViewInfo, providerIcon);
                break;
            }

            case INCOMPLETE_ENCRYPTED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account.getOpenPgpProvider());
                messageView.showMessageEncryptedButIncomplete(messageViewInfo, providerIcon);
                break;
            }

            case ENCRYPTED_ERROR:
            case UNSUPPORTED_ENCRYPTED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account.getOpenPgpProvider());
                messageView.showMessageCryptoErrorView(messageViewInfo, providerIcon);
                break;
            }

            case ENCRYPTED_NO_PROVIDER: {
                messageView.showCryptoProviderNotConfigured(messageViewInfo);
                break;
            }

            case INCOMPLETE_SIGNED:
            case UNSUPPORTED_SIGNED:
            default: {
                messageView.showMessage(account, messageViewInfo);
                break;
            }

            case LOADING: {
                throw new IllegalStateException("Displaying message while in loading state!");
            }
        }

        return true;
    }

    @Override
    public void onCryptoClick() {
        if (cryptoResultAnnotation == null) {
            return;
        }
        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.fromResultAnnotation(cryptoResultAnnotation);
        switch (displayStatus) {
            case LOADING:
                // no need to do anything, there is a progress bar...
                break;
            default:
                displayCryptoInfoDialog(displayStatus);
                break;
        }
    }

    @SuppressWarnings("UnusedParameters") // for consistency with Activity.onActivityResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_UNKNOWN_KEY) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }

            messageCryptoMvpView.restartMessageCryptoProcessing();
        } else if (requestCode == REQUEST_CODE_SECURITY_WARNING) {
            messageCryptoMvpView.redisplayMessage();
        } else {
            throw new IllegalStateException("got an activity result that wasn't meant for us. this is a bug!");
        }
    }

    private void displayCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus) {
        messageCryptoMvpView.showCryptoInfoDialog(
                displayStatus, cryptoResultAnnotation.hasOpenPgpInsecureWarningPendingIntent());
    }

    void onClickSearchKey() {
        try {
            PendingIntent pendingIntent = cryptoResultAnnotation.getOpenPgpSigningKeyIntentIfAny();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(
                        pendingIntent.getIntentSender(), REQUEST_CODE_UNKNOWN_KEY, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Timber.e(e, "SendIntentException");
        }
    }

    public void onClickShowCryptoKey() {
        try {
            PendingIntent pendingIntent = cryptoResultAnnotation.getOpenPgpSigningKeyIntentIfAny();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(
                        pendingIntent.getIntentSender(), null, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Timber.e(e, "SendIntentException");
        }
    }

    public void onClickRetryCryptoOperation() {
        messageCryptoMvpView.restartMessageCryptoProcessing();
    }

    public void onClickShowCryptoWarningDetails() {
        try {
            PendingIntent pendingIntent = cryptoResultAnnotation.getOpenPgpInsecureWarningPendingIntent();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(
                        pendingIntent.getIntentSender(), REQUEST_CODE_SECURITY_WARNING, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Timber.e(e, "SendIntentException");
        }
    }

    public Parcelable getDecryptionResultForReply() {
        if (cryptoResultAnnotation != null && cryptoResultAnnotation.isOpenPgpResult()) {
            return cryptoResultAnnotation.getOpenPgpDecryptionResult();
        }
        return null;
    }

    @Nullable
    private static Drawable getOpenPgpApiProviderIcon(Context context, String openPgpProvider) {
        try {
            if (TextUtils.isEmpty(openPgpProvider)) {
                return null;
            }
            return context.getPackageManager().getApplicationIcon(openPgpProvider);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public void onClickConfigureProvider() {
        reloadOnResumeWithoutRecreateFlag = true;
        messageCryptoMvpView.showCryptoConfigDialog();
    }

    public interface MessageCryptoMvpView {
        void redisplayMessage();
        void restartMessageCryptoProcessing();

        void startPendingIntentForCryptoPresenter(IntentSender si, Integer requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) throws IntentSender.SendIntentException;

        void showCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus, boolean hasSecurityWarning);
        void showCryptoConfigDialog();
    }
}
