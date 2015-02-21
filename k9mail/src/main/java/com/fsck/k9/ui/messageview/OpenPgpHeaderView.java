
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
    private Context mContext;
    private OpenPgpHeaderViewCallback callback;

    private OpenPgpSignatureResult signatureResult;
    private boolean encrypted;
    private PendingIntent pendingIntent;

    private ImageView mResultEncryptionIcon;
    private TextView mResultEncryptionText;
    private ImageView mResultSignatureIcon;
    private TextView mResultSignatureText;
    private LinearLayout mResultSignatureLayout;
    private TextView mResultSignatureName;
    private TextView mResultSignatureEmail;
    private Button mResultSignatureButton;

    public OpenPgpHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onFinishInflate() {
        mResultEncryptionIcon = (ImageView) findViewById(R.id.result_encryption_icon);
        mResultEncryptionText = (TextView) findViewById(R.id.result_encryption_text);
        mResultSignatureIcon = (ImageView) findViewById(R.id.result_signature_icon);
        mResultSignatureText = (TextView) findViewById(R.id.result_signature_text);
        mResultSignatureLayout = (LinearLayout) findViewById(R.id.result_signature_layout);
        mResultSignatureName = (TextView) findViewById(R.id.result_signature_name);
        mResultSignatureEmail = (TextView) findViewById(R.id.result_signature_email);
        mResultSignatureButton = (Button) findViewById(R.id.result_signature_button);
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
            mResultSignatureButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onPgpSignatureButtonClick(pendingIntent);
                }
            });
        } else {
            mResultSignatureButton.setVisibility(View.GONE);
            mResultSignatureButton.setOnClickListener(null);
        }

        if (encrypted) {
            setStatusImageAndTextColor(mContext, mResultEncryptionIcon, mResultEncryptionText, CryptoState.ENCRYPTED);
            mResultEncryptionText.setText(R.string.openpgp_result_encrypted);
        } else {
            setStatusImageAndTextColor(mContext, mResultEncryptionIcon, mResultEncryptionText, CryptoState.NOT_ENCRYPTED);
            mResultEncryptionText.setText(R.string.openpgp_result_not_encrypted);
        }

        if (signatureResult == null) {
            setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.NOT_SIGNED);
            mResultSignatureText.setText(R.string.openpgp_result_no_signature);
            mResultSignatureLayout.setVisibility(View.GONE);
        } else {
            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.INVALID);
                    mResultSignatureText.setText(R.string.openpgp_result_invalid_signature);

                    mResultSignatureLayout.setVisibility(View.GONE);
                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.VERIFIED);
                    mResultSignatureText.setText(R.string.openpgp_result_signature_certified);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        mResultSignatureButton.setVisibility(View.VISIBLE);
                        mResultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    mResultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.UNKNOWN_KEY);
                    mResultSignatureText.setText(R.string.openpgp_result_signature_missing_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        mResultSignatureButton.setVisibility(View.VISIBLE);
                        mResultSignatureButton.setText(R.string.openpgp_result_action_lookup);
                    }
                    mResultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.UNVERIFIED);
                    mResultSignatureText.setText(R.string.openpgp_result_signature_uncertified);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        mResultSignatureButton.setVisibility(View.VISIBLE);
                        mResultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    mResultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.EXPIRED);
                    mResultSignatureText.setText(R.string.openpgp_result_signature_expired_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        mResultSignatureButton.setVisibility(View.VISIBLE);
                        mResultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    mResultSignatureLayout.setVisibility(View.VISIBLE);

                    break;
                }
                case OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED: {
                    setStatusImageAndTextColor(mContext, mResultSignatureIcon, mResultSignatureText, CryptoState.REVOKED);
                    mResultSignatureText.setText(R.string.openpgp_result_signature_revoked_key);

                    setUserId(signatureResult);
                    if (pendingIntent != null) {
                        mResultSignatureButton.setVisibility(View.VISIBLE);
                        mResultSignatureButton.setText(R.string.openpgp_result_action_show);
                    }
                    mResultSignatureLayout.setVisibility(View.VISIBLE);

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
            mResultSignatureName.setText(userInfo.name);
        } else {
            mResultSignatureName.setText(R.string.openpgp_result_no_name);
        }
        if (userInfo.email != null) {
            mResultSignatureEmail.setText(userInfo.email);
        } else {
            mResultSignatureEmail.setText(R.string.openpgp_result_no_email);
        }
    }

    private void setStatusImageAndTextColor(Context context, ImageView statusIcon, TextView statusText,
            CryptoState state) {

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
