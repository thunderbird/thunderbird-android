
package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.mailstore.OpenPgpResultAnnotation;
import com.fsck.k9.mailstore.SmimeResultAnnotation;

import org.openintents.smime.SmimeDecryptionResult;
import org.openintents.smime.SmimeError;
import org.openintents.smime.SmimeSignatureResult;
import org.openintents.smime.util.SmimeUtils;


public class SmimeHeaderView extends LinearLayout {
    private Context context;
    private SmimeHeaderViewCallback callback;

    private SmimeResultAnnotation cryptoAnnotation;

    private ImageView resultEncryptionIcon;
    private TextView resultEncryptionText;
    private ImageView resultSignatureIcon;
    private TextView resultSignatureText;
    private LinearLayout resultSignatureLayout;
    private TextView resultSignatureName;
    private TextView resultSignatureEmail;
    private Button resultSignatureButton;


    public SmimeHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        resultEncryptionIcon = (ImageView) findViewById(R.id.result_encryption_icon);
        resultEncryptionText = (TextView) findViewById(R.id.result_encryption_text);
        resultSignatureIcon = (ImageView) findViewById(R.id.result_signature_icon);
        resultSignatureText = (TextView) findViewById(R.id.result_signature_text);
        resultSignatureLayout = (LinearLayout) findViewById(R.id.result_signature_layout);
        resultSignatureName = (TextView) findViewById(R.id.result_signature_name);
        resultSignatureEmail = (TextView) findViewById(R.id.result_signature_email);
        resultSignatureButton = (Button) findViewById(R.id.result_signature_button);
    }

    public void setCallback(SmimeHeaderViewCallback callback) {
        this.callback = callback;
    }

    public void setSmimeData(SmimeResultAnnotation cryptoAnnotation) {
        this.cryptoAnnotation = cryptoAnnotation;

        initializeEncryptionHeader();
        initializeSignatureHeader();
    }

    private void initializeEncryptionHeader() {
        if (noCryptoAnnotationFound()) {
            displayNotEncrypted();
            return;
        }

        switch (cryptoAnnotation.getErrorType()) {
            case NONE: {
                SmimeDecryptionResult decryptionResult = cryptoAnnotation.getDecryptionResult();
                switch (decryptionResult.getResult()) {
                    case SmimeDecryptionResult.RESULT_NOT_ENCRYPTED: {
                        displayNotEncrypted();
                        break;
                    }
                    case SmimeDecryptionResult.RESULT_ENCRYPTED: {
                        displayEncrypted();
                        break;
                    }
                    case SmimeDecryptionResult.RESULT_INSECURE: {
                        displayInsecure();
                        break;
                    }
                    default:
                        throw new RuntimeException("SmimeDecryptionResult result not handled!");
                }
                break;
            }
            case CRYPTO_API_RETURNED_ERROR: {
                displayEncryptionError();
                break;
            }
            case ENCRYPTED_BUT_INCOMPLETE: {
                displayIncompleteEncryptedPart();
                break;
            }
            case SIGNED_BUT_INCOMPLETE: {
                displayNotEncrypted();
                break;
            }
        }
    }

    private boolean noCryptoAnnotationFound() {
        return cryptoAnnotation == null;
    }

    private void displayEncrypted() {
        setEncryptionImageAndTextColor(CryptoState.ENCRYPTED);
        resultEncryptionText.setText(R.string.openpgp_result_encrypted);
    }

    private void displayNotEncrypted() {
        setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
        resultEncryptionText.setText(R.string.openpgp_result_not_encrypted);
    }

    private void displayInsecure() {
        setEncryptionImageAndTextColor(CryptoState.INVALID);
        resultEncryptionText.setText(R.string.openpgp_result_decryption_insecure);
    }

    private void displayEncryptionError() {
        setEncryptionImageAndTextColor(CryptoState.INVALID);

        SmimeError error = cryptoAnnotation.getError();
        String text;
        if (error == null) {
            text = context.getString(R.string.smime_unknown_error);
        } else {
            text = context.getString(R.string.smime_decryption_failed, error.getMessage());
        }
        resultEncryptionText.setText(text);
    }

    private void displayIncompleteEncryptedPart() {
        setEncryptionImageAndTextColor(CryptoState.UNAVAILABLE);
        resultEncryptionText.setText(R.string.crypto_incomplete_message);
    }

    private void initializeSignatureHeader() {
        initializeSignatureButton();

        if (noCryptoAnnotationFound()) {
            displayNotSigned();
            return;
        }

        switch (cryptoAnnotation.getErrorType()) {
            case CRYPTO_API_RETURNED_ERROR:
                displayEncryptionError();
                hideVerificationState();
                break;
            case NONE: {
                displayVerificationResult();
                break;
            }
            case ENCRYPTED_BUT_INCOMPLETE:
            case SIGNED_BUT_INCOMPLETE: {
                displayIncompleteSignedPart();
                break;
            }
        }
    }

    private void hideVerificationState() {
        hideSignatureLayout();
        resultSignatureText.setVisibility(View.GONE);
        resultSignatureIcon.setVisibility(View.GONE);
    }

    private void displayIncompleteSignedPart() {
        setSignatureImageAndTextColor(CryptoState.UNAVAILABLE);
        resultSignatureText.setText(R.string.crypto_incomplete_message);
        hideSignatureLayout();
    }

    private void displayVerificationResult() {
        SmimeSignatureResult signatureResult = cryptoAnnotation.getSignatureResult();

        switch (signatureResult.getResult()) {
            case SmimeSignatureResult.RESULT_NO_SIGNATURE: {
                displayNotSigned();
                break;
            }
            case SmimeSignatureResult.RESULT_INVALID_SIGNATURE: {
                displaySignatureError();
                break;
            }
            case SmimeSignatureResult.RESULT_VALID_CONFIRMED: {
                displaySignatureSuccessCertified();
                break;
            }
            case SmimeSignatureResult.RESULT_KEY_MISSING: {
                displaySignatureKeyMissing();
                break;
            }
            case SmimeSignatureResult.RESULT_VALID_UNCONFIRMED: {
                displaySignatureSuccessUncertified();
                break;
            }
            case SmimeSignatureResult.RESULT_INVALID_KEY_EXPIRED: {
                displaySignatureKeyExpired();
                break;
            }
            case SmimeSignatureResult.RESULT_INVALID_KEY_REVOKED: {
                displaySignatureKeyRevoked();
                break;
            }
            case SmimeSignatureResult.RESULT_INVALID_INSECURE: {
                displaySignatureInsecure();
                break;
            }
            default: {
                throw new RuntimeException("SmimeSignatureResult result not handled!");
            }
        }
    }

    private void initializeSignatureButton() {
        if (noCryptoAnnotationFound()) {
            hideSignatureButton();
        } else if (isSignatureButtonUsed()) {
            setSignatureButtonClickListener();
        } else {
            hideSignatureButton();
        }
    }

    private boolean isSignatureButtonUsed() {
        return cryptoAnnotation.getPendingIntent() != null;
    }

    private void setSignatureButtonClickListener() {
        final PendingIntent pendingIntent = cryptoAnnotation.getPendingIntent();
        resultSignatureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSmimeSignatureButtonClick(pendingIntent);
            }
        });
    }

    private void hideSignatureButton() {
        resultSignatureButton.setVisibility(View.GONE);
        resultSignatureButton.setOnClickListener(null);
    }

    private void showSignatureButtonWithTextIfNecessary(@StringRes int stringId) {
        if (isSignatureButtonUsed()) {
            resultSignatureButton.setVisibility(View.VISIBLE);
            resultSignatureButton.setText(stringId);
        }
    }

    private void displayNotSigned() {
        setSignatureImageAndTextColor(CryptoState.NOT_SIGNED);
        resultSignatureText.setText(R.string.smime_result_no_signature);
        hideSignatureLayout();
    }

    private void displaySignatureError() {
        setSignatureImageAndTextColor(CryptoState.INVALID);
        resultSignatureText.setText(R.string.smime_result_invalid_signature);
        hideSignatureLayout();
    }

    private void displaySignatureSuccessCertified() {
        setSignatureImageAndTextColor(CryptoState.VERIFIED);
        resultSignatureText.setText(R.string.smime_result_signature_certified);

        displayUserIdAndSignatureButton();
    }

    private void displaySignatureKeyMissing() {
        setSignatureImageAndTextColor(CryptoState.UNKNOWN_KEY);
        resultSignatureText.setText(R.string.smime_result_signature_missing_key);

        setUserId(cryptoAnnotation.getSignatureResult());
        showSignatureButtonWithTextIfNecessary(R.string.smime_result_action_lookup);
        showSignatureLayout();
    }

    private void displaySignatureSuccessUncertified() {
        setSignatureImageAndTextColor(CryptoState.UNVERIFIED);
        resultSignatureText.setText(R.string.smime_result_signature_uncertified);

        displayUserIdAndSignatureButton();
    }

    private void displaySignatureKeyExpired() {
        setSignatureImageAndTextColor(CryptoState.EXPIRED);
        resultSignatureText.setText(R.string.smime_result_signature_expired_key);

        displayUserIdAndSignatureButton();
    }

    private void displaySignatureKeyRevoked() {
        setSignatureImageAndTextColor(CryptoState.REVOKED);
        resultSignatureText.setText(R.string.smime_result_signature_revoked_key);

        displayUserIdAndSignatureButton();
    }

    private void displaySignatureInsecure() {
        setSignatureImageAndTextColor(CryptoState.INVALID);
        resultSignatureText.setText(R.string.smime_result_signature_insecure);

        displayUserIdAndSignatureButton();
    }

    private void displayUserIdAndSignatureButton() {
        setUserId(cryptoAnnotation.getSignatureResult());
        showSignatureButtonWithTextIfNecessary(R.string.smime_result_action_show);
        showSignatureLayout();
    }

    private void setUserId(SmimeSignatureResult signatureResult) {
        final SmimeUtils.UserId userInfo = SmimeUtils.splitUserId(signatureResult.getPrimaryUserId());
        if (userInfo.name != null) {
            resultSignatureName.setText(userInfo.name);
        } else {
            resultSignatureName.setText(R.string.openpgp_result_no_name);
        }

        if (userInfo.email != null) {
            resultSignatureEmail.setText(userInfo.email);
        } else {
            resultSignatureEmail.setText(R.string.openpgp_result_no_email);
        }
    }

    private void hideSignatureLayout() {
        resultSignatureLayout.setVisibility(View.GONE);
    }

    private void showSignatureLayout() {
        resultSignatureLayout.setVisibility(View.VISIBLE);
    }

    private void setEncryptionImageAndTextColor(CryptoState state) {
        setStatusImageAndTextColor(resultEncryptionIcon, resultEncryptionText, state);
    }

    private void setSignatureImageAndTextColor(CryptoState state) {
        setStatusImageAndTextColor(resultSignatureIcon, resultSignatureText, state);
    }

    private void setStatusImageAndTextColor(ImageView statusIcon, TextView statusText, CryptoState state) {
        Drawable statusImageDrawable = context.getResources().getDrawable(state.getDrawableId());
        statusIcon.setImageDrawable(statusImageDrawable);

        int color = context.getResources().getColor(state.getColorId());
        statusIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (statusText != null) {
            statusText.setTextColor(color);
        }
    }


    private enum CryptoState {
        VERIFIED(R.drawable.status_signature_verified_cutout, R.color.smime_green),
        ENCRYPTED(R.drawable.status_lock_closed, R.color.smime_green),

        UNAVAILABLE(R.drawable.status_signature_unverified_cutout, R.color.smime_orange),
        UNVERIFIED(R.drawable.status_signature_unverified_cutout, R.color.smime_orange),
        UNKNOWN_KEY(R.drawable.status_signature_unknown_cutout, R.color.smime_orange),

        REVOKED(R.drawable.status_signature_revoked_cutout, R.color.smime_red),
        EXPIRED(R.drawable.status_signature_expired_cutout, R.color.smime_red),
        NOT_ENCRYPTED(R.drawable.status_lock_open, R.color.smime_red),
        NOT_SIGNED(R.drawable.status_signature_unknown_cutout, R.color.smime_red),
        INVALID(R.drawable.status_signature_invalid_cutout, R.color.smime_red);


        private final int drawableId;
        private final int colorId;

        CryptoState(@DrawableRes int drawableId, @ColorRes int colorId) {
            this.drawableId = drawableId;
            this.colorId = colorId;
        }

        @DrawableRes
        public int getDrawableId() {
            return drawableId;
        }

        @ColorRes
        public int getColorId() {
            return colorId;
        }
    }
}
