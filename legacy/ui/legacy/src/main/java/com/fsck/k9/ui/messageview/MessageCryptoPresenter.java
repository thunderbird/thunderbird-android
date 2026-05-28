package com.fsck.k9.ui.messageview;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import java.util.List;
import com.ciphermail.smime.api.SmimeDecryptionResult;
import com.ciphermail.smime.api.SmimeSignatureResult;
import com.ciphermail.smime.api.util.SmimeApi;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.ui.R;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.MessageHeader;
import net.thunderbird.core.android.account.LegacyAccountDto;
import net.thunderbird.core.logging.legacy.Log;


@SuppressWarnings("WeakerAccess")
public class MessageCryptoPresenter {
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

    public CryptoResultAnnotation getCryptoResultAnnotation() {
        return cryptoResultAnnotation;
    }

    public void onResume() {
        if (reloadOnResumeWithoutRecreateFlag) {
            reloadOnResumeWithoutRecreateFlag = false;
            messageCryptoMvpView.restartMessageCryptoProcessing();
        }
    }

    public boolean maybeHandleShowMessage(MessageTopView messageView, LegacyAccountDto account, MessageViewInfo messageViewInfo) {
        this.cryptoResultAnnotation = messageViewInfo.cryptoResultAnnotation;

        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.fromResultAnnotation(messageViewInfo.cryptoResultAnnotation);
        if (displayStatus == MessageCryptoDisplayStatus.DISABLED) {
            return false;
        }

        CryptoResultAnnotation annotation = messageViewInfo.cryptoResultAnnotation;
        if (annotation != null && annotation.isSmimeResult()) {
            showSmimeHeaderStatus(messageView, account, annotation);
        } else {
            messageView.getMessageHeaderView().setCryptoStatus(displayStatus);
        }

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
                Drawable providerIcon = getCryptoProviderIcon(messageView.getContext(), account, messageViewInfo.cryptoResultAnnotation);
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

    /**
     * Render the S/MIME-specific header indicators: separate encrypted/signed icons plus an
     * "open in CipherMail" action when the provider is installed. Replaces the single combined
     * badge for S/MIME messages so the user can tell encryption and signing apart.
     */
    private void showSmimeHeaderStatus(MessageTopView messageView, LegacyAccountDto account,
            CryptoResultAnnotation annotation) {
        SmimeDecryptionResult decryptionResult = annotation.getSmimeDecryptionResult();
        SmimeSignatureResult signatureResult = annotation.getSmimeSignatureResult();

        boolean encrypted = decryptionResult != null
                && decryptionResult.getResult() == SmimeDecryptionResult.RESULT_ENCRYPTED;

        int signedIconRes = 0;
        int signedColorAttr = 0;
        if (signatureResult != null) {
            switch (signatureResult.getResult()) {
                case SmimeSignatureResult.RESULT_VALID_TRUSTED:
                    signedIconRes = R.drawable.status_signature_dots_3;
                    signedColorAttr = R.attr.openpgp_green;
                    break;
                case SmimeSignatureResult.RESULT_VALID_UNTRUSTED:
                case SmimeSignatureResult.RESULT_CERT_MISSING:
                    signedIconRes = R.drawable.status_signature_dots_3;
                    signedColorAttr = R.attr.openpgp_orange;
                    break;
                case SmimeSignatureResult.RESULT_INVALID_SIGNATURE:
                case SmimeSignatureResult.RESULT_CERT_EXPIRED:
                case SmimeSignatureResult.RESULT_CERT_REVOKED:
                    signedIconRes = R.drawable.status_lock_error;
                    signedColorAttr = R.attr.openpgp_grey;
                    break;
                default: // RESULT_NO_SIGNATURE and anything else: no signature icon
                    break;
            }
        }

        String providerPackage = resolveSmimeProviderPackage(messageView.getContext(), account);

        MessageHeader header = messageView.getMessageHeaderView();
        header.setSmimeCryptoStatus(encrypted, signedIconRes, signedColorAttr, providerPackage != null);
        if (providerPackage != null) {
            header.setOpenInSmimeProviderClickListener(
                    v -> messageCryptoMvpView.onClickOpenMessageInSmimeProvider(providerPackage));
        }
    }

    /**
     * Resolve the installed S/MIME provider package. Prefers the account's configured
     * provider, but falls back to any installed app exposing the S/MIME service — the
     * account's {@code smimeProvider} can be null even when S/MIME works and CipherMail
     * is installed (S/MIME-enabled is tracked independently of the resolved provider).
     */
    @Nullable
    private String resolveSmimeProviderPackage(Context context, LegacyAccountDto account) {
        PackageManager packageManager = context.getPackageManager();

        String providerPackage = account.getSmimeProvider();
        if (providerPackage != null) {
            try {
                packageManager.getPackageInfo(providerPackage, 0);
                return providerPackage;
            } catch (PackageManager.NameNotFoundException e) {
                // configured provider is gone; fall back to discovery below
            }
        }

        Intent serviceIntent = new Intent(SmimeApi.SERVICE_INTENT);
        List<ResolveInfo> services = packageManager.queryIntentServices(serviceIntent, 0);
        for (ResolveInfo resolveInfo : services) {
            if (resolveInfo.serviceInfo != null) {
                return resolveInfo.serviceInfo.packageName;
            }
        }
        return null;
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

    void onClickSearchKey() {
        try {
            PendingIntent pendingIntent = cryptoResultAnnotation.getOpenPgpSigningKeyIntentIfAny();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(
                    pendingIntent.getIntentSender(), REQUEST_CODE_UNKNOWN_KEY);
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(e, "SendIntentException");
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
                        pendingIntent.getIntentSender(), REQUEST_CODE_SECURITY_WARNING);
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(e, "SendIntentException");
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

    @Nullable
    private static Drawable getCryptoProviderIcon(Context context, LegacyAccountDto account,
            CryptoResultAnnotation annotation) {
        String providerPackage;
        if (annotation != null && isSmimeCryptoError(annotation)) {
            providerPackage = account.getSmimeProvider();
        } else {
            providerPackage = account.getOpenPgpProvider();
        }
        return getOpenPgpApiProviderIcon(context, providerPackage);
    }

    private static boolean isSmimeCryptoError(CryptoResultAnnotation annotation) {
        switch (annotation.getErrorType()) {
            case SMIME_OK:
            case SMIME_SIGNED_API_ERROR:
            case SMIME_ENCRYPTED_API_ERROR:
            case SMIME_ENCRYPTED_NO_PROVIDER:
                return true;
            default:
                return false;
        }
    }

    public void onClickConfigureProvider() {
        reloadOnResumeWithoutRecreateFlag = true;
        messageCryptoMvpView.showCryptoConfigDialog();
    }

    public interface MessageCryptoMvpView {
        void redisplayMessage();
        void restartMessageCryptoProcessing();

        void startPendingIntentForCryptoPresenter(IntentSender intentSender, Integer requestCode)
            throws IntentSender.SendIntentException;

        void showCryptoConfigDialog();

        /** Hand the raw message to the given S/MIME provider package for inspection. */
        void onClickOpenMessageInSmimeProvider(String providerPackage);
    }
}
