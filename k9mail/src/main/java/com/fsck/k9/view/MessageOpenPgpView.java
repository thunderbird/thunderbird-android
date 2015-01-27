
package com.fsck.k9.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.crypto.CryptoHelper;
import com.fsck.k9.crypto.OpenPgpApiHelper;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

public class MessageOpenPgpView extends LinearLayout {

    private Context mContext;
    private MessageViewFragment mFragment;
    private RelativeLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mSignatureUserId = null;
    private TextView mText = null;
    private ProgressBar mProgress;
    private Button mGetKeyButton;

    private OpenPgpServiceConnection mOpenPgpServiceConnection;
    private OpenPgpApi mOpenPgpApi;

    private String mOpenPgpProvider;
    private Message mMessage;

    private PendingIntent mMissingKeyPI;

    private static final int REQUEST_CODE_DECRYPT_VERIFY = 12;

    String mData;
    Account mAccount;

    public MessageOpenPgpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setupChildViews() {
        mSignatureLayout = (RelativeLayout) findViewById(R.id.openpgp_signature_layout);
        mSignatureStatusImage = (ImageView) findViewById(R.id.openpgp_signature_status);
        mSignatureUserId = (TextView) findViewById(R.id.openpgp_user_id);
        mText = (TextView) findViewById(R.id.openpgp_text);
        mProgress = (ProgressBar) findViewById(R.id.openpgp_progress);
        mGetKeyButton = (Button) findViewById(R.id.openpgp_get_key);

        mGetKeyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getMissingKey();
            }
        });
    }

    public void setFragment(Fragment fragment) {
        mFragment = (MessageViewFragment) fragment;
    }

    /**
     * Fill the decrypt layout with signature data, if known, make controls
     * visible, if they should be visible.
     */
    public void updateLayout(Account account, String decryptedData,
            final OpenPgpSignatureResult signatureResult,
            final Message message) {

        // set class variables
        mAccount = account;
        mOpenPgpProvider = mAccount.getOpenPgpProvider();
        mMessage = message;

        // only use this view if a OpenPGP Provider is set
        if (mOpenPgpProvider == null) {
            return;
        }

        Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        // bind to service
        mOpenPgpServiceConnection = new OpenPgpServiceConnection(activity,
                mOpenPgpProvider);
        mOpenPgpServiceConnection.bindToService();

        if ((message == null) && (decryptedData == null)) {
            this.setVisibility(View.GONE);

            // don't process further
            return;
        }
        if (decryptedData != null && signatureResult == null) {
            // encrypted-only

            MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                    R.color.openpgp_blue));
            mText.setText(R.string.openpgp_successful_decryption);

            // don't process further
            return;
        } else if (decryptedData != null && signatureResult != null) {
            // signed-only and signed-and-encrypted

            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_ERROR:
                    // TODO: signature error but decryption works?
                    mText.setText(R.string.openpgp_signature_invalid);
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_red));

                    mGetKeyButton.setVisibility(View.GONE);
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.GONE);
                    break;

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED:
                    if (signatureResult.isSignatureOnly()) {
                        mText.setText(R.string.openpgp_signature_valid_certified);
                    }
                    else {
                        mText.setText(R.string.openpgp_successful_decryption_valid_signature_certified);
                    }
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_green));

                    mGetKeyButton.setVisibility(View.GONE);
                    mSignatureUserId.setText(signatureResult.getPrimaryUserId());
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    mSignatureLayout.setVisibility(View.VISIBLE);

                    break;

                case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING:
                    if (signatureResult.isSignatureOnly()) {
                        mText.setText(R.string.openpgp_signature_unknown_text);
                    }
                    else {
                        mText.setText(R.string.openpgp_successful_decryption_unknown_signature);
                    }
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_orange));
                    
                    mGetKeyButton.setVisibility(View.VISIBLE);
                    mSignatureUserId.setText(R.string.openpgp_signature_unknown);
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mSignatureLayout.setVisibility(View.VISIBLE);

                    break;

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED:
                    if (signatureResult.isSignatureOnly()) {
                        mText.setText(R.string.openpgp_signature_valid_uncertified);
                    }
                    else {
                        mText.setText(R.string.openpgp_successful_decryption_valid_signature_uncertified);
                    }
                    MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                            R.color.openpgp_orange));

                    mGetKeyButton.setVisibility(View.GONE);
                    mSignatureUserId.setText(signatureResult.getPrimaryUserId());
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
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
                Part pgp = MimeUtility.findFirstPartByMimeType(message, "application/pgp-encrypted");
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
                    // get data String
                    Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
                    if (part == null) {
                        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
                    }
                    if (part != null) {
                        mData = MessageExtractor.getTextFromPart(part);
                    }

                    // wait for service to be bound
                    while (!mOpenPgpServiceConnection.isBound()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }

                    mOpenPgpApi = new OpenPgpApi(getContext(),
                            mOpenPgpServiceConnection.getService());

                    decryptVerify(new Intent());

                } catch (MessagingException me) {
                    Log.e(K9.LOG_TAG, "Unable to decrypt email.", me);
                }

            }
        };

        new Thread(r).start();
    }

    private void decryptVerify(Intent intent) {
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
        intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(mAccount, mMessage);
        String accName = OpenPgpApiHelper.buildAccountName(identity);
        intent.putExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME, accName);

        InputStream is = new ByteArrayInputStream(mData.getBytes(Charset.forName("UTF-8")));
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        DecryptVerifyCallback callback = new DecryptVerifyCallback(os, REQUEST_CODE_DECRYPT_VERIFY);

        mOpenPgpApi.executeApiAsync(intent, is, os, callback);
    }

    private void getMissingKey() {
        try {
            mFragment.getActivity().startIntentSenderForResult(
                    mMissingKeyPI.getIntentSender(),
                    REQUEST_CODE_DECRYPT_VERIFY, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Log.e(K9.LOG_TAG, "SendIntentException", e);
        }
    }

    /**
     * Called on successful decrypt/verification
     */
    private class DecryptVerifyCallback implements OpenPgpApi.IOpenPgpCallback {
        ByteArrayOutputStream os;
        int requestCode;

        private DecryptVerifyCallback(ByteArrayOutputStream os, int requestCode) {
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {
                    try {
                        final String output = os.toString("UTF-8");

                        OpenPgpSignatureResult sigResult = null;
                        if (result.hasExtra(OpenPgpApi.RESULT_SIGNATURE)) {
                            sigResult = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                        }

                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "result: " + os.toByteArray().length
                                    + " str=" + output);

                        // missing key -> PendingIntent to get keys
                        mMissingKeyPI = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                        mProgress.setVisibility(View.GONE);
                        mFragment.setMessageWithOpenPgp(output, sigResult);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(K9.LOG_TAG, "UnsupportedEncodingException", e);
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    try {
                        mFragment.getActivity().startIntentSenderForResult(
                                pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (SendIntentException e) {
                        Log.e(K9.LOG_TAG, "SendIntentException", e);
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    handleError(error);
                    break;
                }
            }
        }
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "onActivityResult resultCode: " + resultCode);

        // try again after user interaction
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_DECRYPT_VERIFY) {
            /*
             * The data originally given to the decryptVerify() method, is again
             * returned here to be used when calling decryptVerify() after user
             * interaction. The Intent now also contains results from the user
             * interaction, for example selected key ids.
             */
            decryptVerify(data);

            return true;
        }

        return false;
    }

    private void handleError(final OpenPgpError error) {
        Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mProgress.setVisibility(View.GONE);

                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "OpenPGP Error ID:" + error.getErrorId());
                    Log.d(K9.LOG_TAG, "OpenPGP Error Message:" + error.getMessage());
                }

                mText.setText(mFragment.getString(R.string.openpgp_error) + " "
                        + error.getMessage());
                MessageOpenPgpView.this.setBackgroundColor(mFragment.getResources().getColor(
                        R.color.openpgp_red));
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // bind to service if a OpenPGP Provider is available
        if (mOpenPgpProvider != null) {
            mOpenPgpServiceConnection = new OpenPgpServiceConnection(mFragment.getActivity(),
                    mOpenPgpProvider);
            mOpenPgpServiceConnection.bindToService();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mOpenPgpServiceConnection != null) {
            mOpenPgpServiceConnection.unbindFromService();
        }
    }

}
