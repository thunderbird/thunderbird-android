package com.fsck.k9.ui.messageview;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageCryptoDisplayStatus;


public class MessageCryptoPresenter implements OnCryptoClickListener {
    public static final int REQUEST_CODE_UNKNOWN_KEY = 123;


    // injected state
    private final MessageCryptoMvpView messageCryptoMvpView;


    // persistent state
    private boolean overrideCryptoWarning;


    // transient state
    private CryptoResultAnnotation cryptoResultAnnotation;


    public MessageCryptoPresenter(Bundle savedInstanceState, MessageCryptoMvpView messageCryptoMvpView) {
        this.messageCryptoMvpView = messageCryptoMvpView;

        if (savedInstanceState != null) {
            overrideCryptoWarning = savedInstanceState.getBoolean("overrideCryptoWarning");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("overrideCryptoWarning", overrideCryptoWarning);
    }

    public boolean maybeHandleShowMessage(MessageTopView messageView, Account account, MessageViewInfo messageViewInfo) {
        this.cryptoResultAnnotation = messageViewInfo.cryptoResultAnnotation;

        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.fromResultAnnotation(messageViewInfo.cryptoResultAnnotation);
        if (displayStatus == MessageCryptoDisplayStatus.DISABLED) {
            return false;
        }

        boolean suppressSignOnlyMessages = !account.getCryptoSupportSignOnly();
        if (suppressSignOnlyMessages && displayStatus.isUnencryptedSigned()) {
            return false;
        }

        messageView.getMessageHeaderView().setCryptoStatus(displayStatus);

        switch (displayStatus) {
            case UNENCRYPTED_SIGN_REVOKED:
            case ENCRYPTED_SIGN_REVOKED: {
                showMessageCryptoWarning(messageView, account, messageViewInfo,
                        R.string.messageview_crypto_warning_revoked);
                break;
            }
            case UNENCRYPTED_SIGN_EXPIRED:
            case ENCRYPTED_SIGN_EXPIRED: {
                showMessageCryptoWarning(messageView, account, messageViewInfo,
                        R.string.messageview_crypto_warning_expired);
                break;
            }
            case UNENCRYPTED_SIGN_INSECURE:
            case ENCRYPTED_SIGN_INSECURE: {
                showMessageCryptoWarning(messageView, account, messageViewInfo,
                        R.string.messageview_crypto_warning_insecure);
                break;
            }
            case UNENCRYPTED_SIGN_ERROR:
            case ENCRYPTED_SIGN_ERROR: {
                showMessageCryptoWarning(messageView, account, messageViewInfo,
                        R.string.messageview_crypto_warning_error);
                break;
            }

            case CANCELLED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account);
                messageView.showMessageCryptoCancelledView(messageViewInfo, providerIcon);
                break;
            }

            case INCOMPLETE_ENCRYPTED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account);
                messageView.showMessageEncryptedButIncomplete(messageViewInfo, providerIcon);
                break;
            }

            case ENCRYPTED_ERROR:
            case UNSUPPORTED_ENCRYPTED: {
                Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account);
                messageView.showMessageCryptoErrorView(messageViewInfo, providerIcon);
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

    private void showMessageCryptoWarning(MessageTopView messageView, Account account,
            MessageViewInfo messageViewInfo, @StringRes int warningStringRes) {
        if (overrideCryptoWarning) {
            messageView.showMessage(account, messageViewInfo);
            return;
        }
        Drawable providerIcon = getOpenPgpApiProviderIcon(messageView.getContext(), account);
        messageView.showMessageCryptoWarning(messageViewInfo, providerIcon, warningStringRes);
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
            case UNENCRYPTED_SIGN_UNKNOWN:
                launchPendingIntent(cryptoResultAnnotation);
                break;
            default:
                displayCryptoInfoDialog(displayStatus);
                break;
        }
    }

    @SuppressWarnings("UnusedParameters") // for consistency with Activity.onActivityResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_UNKNOWN_KEY) {
            throw new IllegalStateException("got an activity result that wasn't meant for us. this is a bug!");
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        messageCryptoMvpView.restartMessageCryptoProcessing();
    }

    private void displayCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus) {
        messageCryptoMvpView.showCryptoInfoDialog(displayStatus);
    }

    private void launchPendingIntent(CryptoResultAnnotation cryptoResultAnnotation) {
        try {
            PendingIntent pendingIntent = cryptoResultAnnotation.getOpenPgpPendingIntent();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(
                        pendingIntent.getIntentSender(), REQUEST_CODE_UNKNOWN_KEY, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(K9.LOG_TAG, "SendIntentException", e);
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
            Log.e(K9.LOG_TAG, "SendIntentException", e);
        }
    }

    public void onClickRetryCryptoOperation() {
        messageCryptoMvpView.restartMessageCryptoProcessing();
    }

    public void onClickShowMessageOverrideWarning() {
        overrideCryptoWarning = true;
        messageCryptoMvpView.redisplayMessage();
    }

    public Parcelable getDecryptionResultForReply() {
        if (cryptoResultAnnotation != null && cryptoResultAnnotation.isOpenPgpResult()) {
            return cryptoResultAnnotation.getOpenPgpDecryptionResult();
        }
        return null;
    }

    @Nullable
    private static Drawable getOpenPgpApiProviderIcon(Context context, Account account) {
        try {
            String openPgpProvider = account.getOpenPgpProvider();
            if (Account.NO_OPENPGP_PROVIDER.equals(openPgpProvider)) {
                return null;
            }
            return context.getPackageManager().getApplicationIcon(openPgpProvider);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public interface MessageCryptoMvpView {
        void redisplayMessage();
        void restartMessageCryptoProcessing();

        void startPendingIntentForCryptoPresenter(IntentSender si, Integer requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) throws IntentSender.SendIntentException;

        void showCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus);
    }
}
