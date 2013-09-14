
package com.fsck.k9.view;

import org.openintents.openpgp.IOpenPgpCallback;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpServiceConnection;
import org.openintents.openpgp.OpenPgpSignatureResult;

import android.content.Context;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.crypto.CryptoHelper;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

public class MessageOpenPgpView extends LinearLayout {

    private Context mContext;
    private MessageViewFragment mFragment;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mSignatureUserId = null;
    private TextView mText = null;
    private ProgressBar mProgress;

    private OpenPgpServiceConnection mOpenPgpServiceConnection;

    public MessageOpenPgpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setupChildViews() {
        mSignatureLayout = (LinearLayout) findViewById(R.id.openpgp_signature_layout);
        mSignatureStatusImage = (ImageView) findViewById(R.id.openpgp_signature_status);
        mSignatureUserId = (TextView) findViewById(R.id.openpgp_user_id);
        mText = (TextView) findViewById(R.id.openpgp_text);
        mProgress = (ProgressBar) findViewById(R.id.openpgp_progress);
    }

    public void setFragment(Fragment fragment) {
        mFragment = (MessageViewFragment) fragment;
    }

    /**
     * Fill the decrypt layout with signature data, if known, make controls
     * visible, if they should be visible.
     */
    public void updateLayout(final String openPgpProvider, String decryptedData,
            final OpenPgpSignatureResult signatureResult, final Message message) {
        // only use this view if a OpenPGP Provider is set
        if (openPgpProvider == null) {
            return;
        }

        // bind to service
        mOpenPgpServiceConnection = new OpenPgpServiceConnection(mFragment.getActivity(),
                openPgpProvider);
        mOpenPgpServiceConnection.bindToService();

        if ((message == null) && (decryptedData == null)) {
            this.setVisibility(View.GONE);

            // don't process further
            return;
        }
        if (decryptedData != null && signatureResult == null) {
            // only decrypt
            MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                    R.color.openpgp_blue));
            mText.setText(R.string.openpgp_successful_decryption);

            // don't process further
            return;
        } else if (signatureResult != null && decryptedData != null) {
            // decryptAndVerify / only verify

            switch (signatureResult.getSignatureStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_ERROR:
                    // TODO: signature error but decryption works?
                    mText.setText(R.string.openpgp_signature_invalid);
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_red));

                    // mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.GONE);
                    break;

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS:
                    if (signatureResult.isSignatureOnly()) {
                        mText.setText(R.string.openpgp_signature_valid);
                    }
                    else {
                        mText.setText(R.string.openpgp_successful_decryption_valid_signature);
                    }
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_green));

                    mSignatureUserId.setText(signatureResult.getSignatureUserId());
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    mSignatureLayout.setVisibility(View.VISIBLE);

                    break;

                case OpenPgpSignatureResult.SIGNATURE_UNKNOWN:
                    if (signatureResult.isSignatureOnly()) {
                        mText.setText(R.string.openpgp_signature_unknown_text);
                    }
                    else {
                        mText.setText(R.string.openpgp_successful_decryption_unknown_signature);
                    }
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_orange));

                    mSignatureUserId.setText(R.string.openpgp_signature_unknown);
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.VISIBLE);

                    break;

                default:
                    break;
            }

            // don't process further
            return;
        }

        // Start new decryption/verification
        CryptoHelper helper = new CryptoHelper();
        if (helper.isEncrypted(message) || helper.isSigned(message)) {
            // start automatic decrypt
            decryptAndVerify(message);
        } else {
            try {
                // check for PGP/MIME encryption
                Part pgp = MimeUtility
                        .findFirstPartByMimeType(message, "application/pgp-encrypted");
                if (pgp != null) {
                    Toast.makeText(mContext, R.string.pgp_mime_unsupported, Toast.LENGTH_LONG)
                            .show();
                }
            } catch (MessagingException e) {
                // nothing to do...
            }
        }
    }

    private void decryptAndVerify(final Message message) {
        this.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.VISIBLE);
        MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                R.color.openpgp_orange));
        mText.setText(R.string.openpgp_decrypting_verifying);

        // waiting in a new thread
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    String data = null;
                    Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
                    if (part == null) {
                        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                    }
                    if (part != null) {
                        data = MimeUtility.getTextFromPart(part);
                    }

                    // TODO: handle with callback in cryptoserviceconnection
                    // instead of sleep
                    while (!mOpenPgpServiceConnection.isBound()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }

                    try {
                        mOpenPgpServiceConnection.getService().decryptAndVerify(data.getBytes(),
                                decryptAndVerifyCallback);
                    } catch (RemoteException e) {
                        Log.e(K9.LOG_TAG, "MessageOpenPgpView", e);
                    }
                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Unable to decrypt email.", me);
                }

            }
        };

        new Thread(r).start();

    }

    final IOpenPgpCallback.Stub decryptAndVerifyCallback = new IOpenPgpCallback.Stub() {

        @Override
        public void onSuccess(final byte[] outputBytes, final OpenPgpSignatureResult signatureResult)
                throws RemoteException {

            mFragment.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);

                    mFragment.setMessageWithOpenPgp(new String(outputBytes), signatureResult);
                }
            });

        }

        @Override
        public void onError(final OpenPgpError error) throws RemoteException {
            
            mFragment.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);

                    Log.d(K9.LOG_TAG, "onError getErrorId:" + error.getErrorId());
                    Log.d(K9.LOG_TAG, "onError getMessage:" + error.getMessage());

                    // TODO: better error handling with ids?
                    mText.setText(mFragment.getString(R.string.openpgp_error) + " "
                            + error.getMessage());

                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_red));
                }
            });
        }

    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mOpenPgpServiceConnection != null) {
            mOpenPgpServiceConnection.unbindFromService();
        }
    }

}
