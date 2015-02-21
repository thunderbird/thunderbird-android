
package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpUtils;


public class OpenPgpHeaderView extends LinearLayout {
    private Context context;
    private OpenPgpHeaderViewCallback callback;

    private OpenPgpSignatureResult signatureResult;
    private boolean encrypted;
    private PendingIntent pendingIntent;

    private ImageView resultEncryptionIcon;
    private TextView resultEncryptionText;
    private ImageView resultSignatureIcon;
    private TextView resultSignatureText;
    private LinearLayout resultSignatureLayout;
    private TextView resultSignatureName;
    private TextView resultSignatureEmail;
    private Button resultSignatureButton;


    public OpenPgpHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void onFinishInflate() {
        resultEncryptionIcon = (ImageView) findViewById(R.id.result_encryption_icon);
        resultEncryptionText = (TextView) findViewById(R.id.result_encryption_text);
        resultSignatureIcon = (ImageView) findViewById(R.id.result_signature_icon);
        resultSignatureText = (TextView) findViewById(R.id.result_signature_text);
        resultSignatureLayout = (LinearLayout) findViewById(R.id.result_signature_layout);
        resultSignatureName = (TextView) findViewById(R.id.result_signature_name);
        resultSignatureEmail = (TextView) findViewById(R.id.result_signature_email);
        resultSignatureButton = (Button) findViewById(R.id.result_signature_button);
    }

    public void setOpenPgpData(OpenPgpSignatureResult signatureResult,
            boolean encrypted, PendingIntent pendingIntent) {
        this.signatureResult = signatureResult;
        this.encrypted = encrypted;
        this.pendingIntent = pendingIntent;

        displayOpenPgpView();
    }

    public void setCallback(OpenPgpHeaderViewCallback callback) {
        this.callback = callback;
    }

    public void displayOpenPgpView() {

        if (pendingIntent != null) {
            resultSignatureButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onPgpSignatureButtonClick(pendingIntent);
                }
            });
        } else {
            resultSignatureButton.setVisibility(View.GONE);
            resultSignatureButton.setOnClickListener(null);
        }

        if (encrypted) {
            setEncryptionImageAndTextColor(CryptoState.ENCRYPTED);
            resultEncryptionText.setText(R.string.openpgp_result_encrypted);
        } else {
            setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
            resultEncryptionText.setText(R.string.openpgp_result_not_encrypted);
        }

        if (signatureResult == null) {
            setSignatureImageAndTextColor(CryptoState.NOT_SIGNED);
            resultSignatureText.setText(R.string.openpgp_result_no_signature);
            resultSignatureLayout.setVisibility(View.GONE);
        } else {
            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                    setSignatureImageAndTextColor(CryptoState.INVALID);
                    resultSignatureText.setText(R.string.openpgp_result_invalid_signature);

                    resultSignatureLayout.setVisibility(View.GONE);
                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                    setSignatureImageAndTextColor(CryptoState.VERIFIED);
                    resultSignatureText.setText(R.string.openpgp_result_signature_certified);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        resultSignatureButton.setVisibility(View.VISIBLE);
                        resultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    resultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                    setSignatureImageAndTextColor(CryptoState.UNKNOWN_KEY);
                    resultSignatureText.setText(R.string.openpgp_result_signature_missing_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        resultSignatureButton.setVisibility(View.VISIBLE);
                        resultSignatureButton.setText(R.string.openpgp_result_action_lookup);
                    }
                    resultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    setSignatureImageAndTextColor(CryptoState.UNVERIFIED);
                    resultSignatureText.setText(R.string.openpgp_result_signature_uncertified);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        resultSignatureButton.setVisibility(View.VISIBLE);
                        resultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    resultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED: {
                    setSignatureImageAndTextColor(CryptoState.EXPIRED);
                    resultSignatureText.setText(R.string.openpgp_result_signature_expired_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        resultSignatureButton.setVisibility(View.VISIBLE);
                        resultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    resultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED: {
                    setSignatureImageAndTextColor(CryptoState.REVOKED);
                    resultSignatureText.setText(R.string.openpgp_result_signature_revoked_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        resultSignatureButton.setVisibility(View.VISIBLE);
                        resultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    resultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }

                default:
                    break;
            }

        }
    }

    private void setUserId(OpenPgpSignatureResult signatureResult) {
        final OpenPgpUtils.UserInfo userInfo = OpenPgpUtils.splitUserId(signatureResult.getPrimaryUserId());
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
        VERIFIED(R.drawable.status_signature_verified_cutout, R.color.openpgp_green),
        ENCRYPTED(R.drawable.status_lock_closed, R.color.openpgp_green),

        UNAVAILABLE(R.drawable.status_signature_unverified_cutout, R.color.openpgp_orange),
        UNVERIFIED(R.drawable.status_signature_unverified_cutout, R.color.openpgp_orange),
        UNKNOWN_KEY(R.drawable.status_signature_unknown_cutout, R.color.openpgp_orange),

        REVOKED(R.drawable.status_signature_revoked_cutout, R.color.openpgp_red),
        EXPIRED(R.drawable.status_signature_expired_cutout, R.color.openpgp_red),
        NOT_ENCRYPTED(R.drawable.status_lock_open, R.color.openpgp_red),
        NOT_SIGNED(R.drawable.status_signature_unknown_cutout, R.color.openpgp_red),
        INVALID(R.drawable.status_signature_invalid_cutout, R.color.openpgp_red);

        private final int drawableId;
        private final int colorId;

        CryptoState(int drawableId, int colorId) {
            this.drawableId = drawableId;
            this.colorId = colorId;
        }

        public int getDrawableId() {
            return drawableId;
        }

        public int getColorId() {
            return colorId;
        }
    }
}
