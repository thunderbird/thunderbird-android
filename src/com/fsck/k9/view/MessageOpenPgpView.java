
package com.fsck.k9.view;

import org.openintents.openpgp.IOpenPgpCallback;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpHelper;
import org.openintents.openpgp.OpenPgpServiceConnection;
import org.openintents.openpgp.OpenPgpSignatureResult;

import android.content.Context;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

public class MessageOpenPgpView extends LinearLayout {

    private Context mContext;
    private MessageViewFragment mFragment;
    private Button mDecryptButton;
    private LinearLayout mCryptoSignatureLayout = null;
    private ImageView mCryptoSignatureStatusImage = null;
    private TextView mCryptoSignatureUserId = null;
    private TextView mCryptoSignatureUserIdRest = null;

    private OpenPgpHelper openPgpHelper;
    private OpenPgpServiceConnection mCryptoServiceConnection;

    public MessageOpenPgpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setupChildViews() {
        mCryptoSignatureLayout = (LinearLayout) findViewById(R.id.crypto_signature_openpgp);
        mCryptoSignatureStatusImage = (ImageView) findViewById(R.id.ic_crypto_signature_status_openpgp);
        mCryptoSignatureUserId = (TextView) findViewById(R.id.userId_openpgp);
        mCryptoSignatureUserIdRest = (TextView) findViewById(R.id.userIdRest_openpgp);
        mCryptoSignatureLayout.setVisibility(View.INVISIBLE);
        mDecryptButton = (Button) findViewById(R.id.btn_decrypt_openpgp);
    }

    public void setFragment(Fragment fragment) {
        mFragment = (MessageViewFragment) fragment;
    }

    public void hide() {
        this.setVisibility(View.GONE);
    }

    /**
     * Fill the decrypt layout with signature data, if known, make controls
     * visible, if they should be visible.
     */
    public void updateLayout(final String openPgpProvider, final PgpData pgpData,
            final Message message) {
        openPgpHelper = new OpenPgpHelper(getContext());

        // bind to service
        mCryptoServiceConnection = new OpenPgpServiceConnection(mFragment.getActivity(),
                openPgpProvider);
        mCryptoServiceConnection.bindToService();

        // if (pgpData.getSignatureKeyId() != 0) {
        if (pgpData.getSignatureUserId() != null) {
            // mCryptoSignatureUserIdRest.setText(
            // mContext.getString(R.string.key_id,
            // Long.toHexString(pgpData.getSignatureKeyId() & 0xffffffffL)));
            // String userId = pgpData.getSignatureUserId();
            // if (userId == null) {
            // userId =
            // mContext.getString(R.string.unknown_crypto_signature_user_id);
            // }
            // String chunks[] = userId.split(" <", 2);
            // String name = chunks[0];
            // if (chunks.length > 1) {
            // mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
            // }
            mCryptoSignatureUserId.setText(pgpData.getSignatureUserId());
            if (pgpData.getSignatureSuccess()) {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
            } else if (pgpData.getSignatureUnknown()) {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            } else {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            mCryptoSignatureLayout.setVisibility(View.VISIBLE);
            this.setVisibility(View.VISIBLE);
        } else {
            mCryptoSignatureLayout.setVisibility(View.INVISIBLE);
        }
        if ((message == null) && (pgpData.getDecryptedData() == null)) {
            this.setVisibility(View.GONE);
            return;
        }
        if (pgpData.getDecryptedData() != null) {
            // if (pgpData.getSignatureKeyId() == 0) {
            if (pgpData.getSignatureUserId() == null) {
                this.setVisibility(View.GONE);
            } else {
                // no need to show button after decryption/verification
                mDecryptButton.setVisibility(View.GONE);
            }
            return;
        }

        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String data = null;
                    Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
                    if (part == null) {
                        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                    }
                    if (part != null) {
                        data = MimeUtility.getTextFromPart(part);
                    }
                    // cryptoProvider.decrypt(mFragment, data, pgpData);

                    try {
                        mCryptoServiceConnection.getService().decryptAndVerify(data.getBytes(),
                                decryptAndVerifyCallback);
                    } catch (RemoteException e) {
                        Log.e(K9.LOG_TAG, "CryptoProviderDemo", e);
                    }

                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Unable to decrypt email.", me);
                }
            }
        });

        mDecryptButton.setVisibility(View.VISIBLE);
        if (openPgpHelper.isEncrypted(message)) {
            mDecryptButton.setText(R.string.btn_decrypt);
            this.setVisibility(View.VISIBLE);
        } else if (openPgpHelper.isSigned(message)) {
            mDecryptButton.setText(R.string.btn_verify);
            this.setVisibility(View.VISIBLE);
        } else {
            this.setVisibility(View.GONE);
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

    final IOpenPgpCallback.Stub decryptAndVerifyCallback = new IOpenPgpCallback.Stub() {

        @Override
        public void onSuccess(final byte[] outputBytes, final OpenPgpSignatureResult signatureResult)
                throws RemoteException {
            Log.d(K9.LOG_TAG, "decryptAndVerifyCallback");

            mFragment.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO: get rid of PgpData
                    PgpData data = new PgpData();
                    data.setDecryptedData(new String(outputBytes));

                    if (signatureResult != null) {
                        data.setSignatureUserId(signatureResult.getSignatureUserId());
                        data.setSignatureSuccess(signatureResult.isSignatureSuccess());
                        data.setSignatureUnknown(signatureResult.isSignatureUnknown());
                    }

                    mFragment.setMessageWithPgpData(data);

                }
            });

        }

        @Override
        public void onError(final OpenPgpError error) throws RemoteException {
            // TODO: better handling on error!

            mFragment.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(mFragment.getActivity(),
                            "onError id:" + error.getErrorId() + "\n\n" + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(K9.LOG_TAG, "onError getErrorId:" + error.getErrorId());
                    Log.e(K9.LOG_TAG, "onError getMessage:" + error.getMessage());
                }
            });
        }

    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mCryptoServiceConnection != null) {
            mCryptoServiceConnection.unbindFromService();
        }
    }

}
