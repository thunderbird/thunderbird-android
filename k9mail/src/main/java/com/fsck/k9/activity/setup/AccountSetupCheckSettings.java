
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import com.fsck.k9.ssl.CertificationErrorUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Checks the given settings to make sure that they can be used to send and
 * receive mail.
 * 
 * XXX NOTE: The manifest for this app has it ignore config changes, because
 * it doesn't correctly deal with restarting while its thread is running.
 */
public class AccountSetupCheckSettings extends K9Activity implements OnClickListener,
        ConfirmationDialogFragmentListener{

    public static final int ACTIVITY_REQUEST_CODE = 1;

    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_CHECK_DIRECTION ="checkDirection";

    public enum CheckDirection {
        INCOMING,
        OUTGOING
    }

    private Handler mHandler = new Handler();

    private ProgressBar mProgressBar;

    private TextView mMessageView;

    private Account mAccount;

    private CheckDirection mDirection;

    private boolean mCanceled;

    private boolean mDestroyed;

    public static void actionCheckSettings(Activity context, Account account,
            CheckDirection direction) {
        Intent i = new Intent(context, AccountSetupCheckSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_CHECK_DIRECTION, direction);
        context.startActivityForResult(i, ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_check_settings);
        mMessageView = (TextView)findViewById(R.id.message);
        mProgressBar = (ProgressBar)findViewById(R.id.progress);
        findViewById(R.id.cancel).setOnClickListener(this);

        setMessage(R.string.account_setup_check_settings_retr_info_msg);
        mProgressBar.setIndeterminate(true);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mDirection = (CheckDirection) getIntent().getSerializableExtra(EXTRA_CHECK_DIRECTION);

        new CheckAccountTask(mAccount).execute(mDirection);
    }

    private void handleCertificateValidationException(CertificateValidationException cve) {
        Log.e(K9.LOG_TAG, "Error while testing settings", cve);

        X509Certificate[] chain = cve.getCertChain();
        // Avoid NullPointerException in acceptKeyDialog()
        if (chain != null) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    cve);
        } else {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(cve));
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mCanceled = true;
    }

    private void setMessage(final int resId) {
        mMessageView.setText(getString(resId));
    }

    private void acceptKeyDialog(final int msgResId, final CertificateValidationException ex) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
                String error = parseAndTranslateCertificateError(ex);

                mProgressBar.setIndeterminate(false);

                final X509Certificate[] chain = ex.getCertChain();
                String chainInfo = CertificationErrorUtils.buildChainInfo(chain, mAccount.getStoreUri(), mAccount.getTransportUri());


                // TODO: refactor with DialogFragment.
                // This is difficult because we need to pass through chain[0] for onClick()
                new AlertDialog.Builder(AccountSetupCheckSettings.this)
                .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                .setMessage(getString(msgResId, error) + " " + chainInfo)
                .setCancelable(true)
                .setPositiveButton(
                    getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        acceptCertificate(chain[0]);
                    }
                })
                .setNegativeButton(
                    getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
            }

            private String parseAndTranslateCertificateError(CertificateValidationException ex) {
                String exMessage = getString(R.string.certificate_error_unknown);
                if(ex != null) {
                    String baseMessage = extractUnderlyingMessage(ex);
                    if(baseMessage.startsWith("hostname in certificate didn't match:")) {
                        ArrayList<String> hostnames = CertificationErrorUtils.extractHostnames(baseMessage);
                        String expectedHostname = hostnames.remove(0);
                        return getString(R.string.certificate_error_hostname_did_not_match, expectedHostname, Arrays.toString(hostnames.toArray()));
                    } else {
                        return baseMessage;
                    }
                } else {
                    return exMessage;
                }
            }

            private String extractUnderlyingMessage(Exception ex) {
                if (ex.getCause() != null) {
                    if (ex.getCause().getCause() != null) {
                        return ex.getCause().getCause().getMessage();
                    } else {
                        return ex.getCause().getMessage();
                    }
                } else {
                    return ex.getMessage();
                }
            }
        });
    }

    /**
     * Permanently accepts a certificate for the INCOMING or OUTGOING direction
     * by adding it to the local key store.
     * 
     * @param certificate
     */
    private void acceptCertificate(X509Certificate certificate) {
        try {
            mAccount.addCertificate(mDirection, certificate);
        } catch (CertificateException e) {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }
        AccountSetupCheckSettings.actionCheckSettings(AccountSetupCheckSettings.this, mAccount,
                mDirection);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        setResult(resCode);
        finish();
    }

    private void onCancel() {
        mCanceled = true;
        setMessage(R.string.account_setup_check_settings_canceling_msg);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.cancel:
            onCancel();
            break;
        }
    }

    private void showErrorDialog(final int msgResId, final Object... args) {
        mHandler.post(new Runnable() {
            public void run() {
                showDialogFragment(R.id.dialog_account_setup_error, getString(msgResId, args));
            }
        });
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        if (mDestroyed) {
            return;
        }
        mProgressBar.setIndeterminate(false);

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        getString(R.string.account_setup_failed_dlg_edit_details_action),
                        getString(R.string.account_setup_failed_dlg_continue_action)
                );
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        FragmentTransaction ta = getFragmentManager().beginTransaction();
        ta.add(fragment, getDialogTag(dialogId));
        ta.commitAllowingStateLoss();

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        //fragment.show(ta, getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                finish();
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                mCanceled = false;
                setResult(RESULT_OK);
                finish();
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
        // nothing to do here...
    }

    private String errorMessageForCertificateException(CertificateValidationException e) {
        switch (e.getReason()) {
            case Expired: return getString(R.string.client_certificate_expired, e.getAlias(), e.getMessage());
            case MissingCapability: return getString(R.string.auth_external_error);
            case RetrievalFailure: return getString(R.string.client_certificate_retrieval_failure, e.getAlias());
            case UseMessage: return e.getMessage();
            case Unknown:
            default: return "";
        }
    }

    /**
     * FIXME: Don't use an AsyncTask to perform network operations.
     * See also discussion in https://github.com/k9mail/k-9/pull/560
     */
    private class CheckAccountTask extends AsyncTask<CheckDirection, Integer, Void> {
        private final Account account;

        private CheckAccountTask(Account account) {
            this.account = account;
        }

        @Override
        protected Void doInBackground(CheckDirection... params) {
            final CheckDirection direction = params[0];
            try {
                /*
                 * This task could be interrupted at any point, but network operations can block,
                 * so relying on InterruptedException is not enough. Instead, check after
                 * each potentially long-running operation.
                 */
                if (cancelled()) {
                    return null;
                }

                clearCertificateErrorNotifications(direction);

                checkServerSettings(direction);

                if (cancelled()) {
                    return null;
                }

                setResult(RESULT_OK);
                finish();

            } catch (AuthenticationFailedException afe) {
                Log.e(K9.LOG_TAG, "Error while testing settings", afe);
                showErrorDialog(
                        R.string.account_setup_failed_dlg_auth_message_fmt,
                        afe.getMessage() == null ? "" : afe.getMessage());
            } catch (CertificateValidationException cve) {
                handleCertificateValidationException(cve);
            } catch (Throwable t) {
                Log.e(K9.LOG_TAG, "Error while testing settings", t);
                showErrorDialog(
                        R.string.account_setup_failed_dlg_server_message_fmt,
                        (t.getMessage() == null ? "" : t.getMessage()));
            }
            return null;
        }

        private void clearCertificateErrorNotifications(CheckDirection direction) {
            final MessagingController ctrl = MessagingController.getInstance(getApplication());
            ctrl.clearCertificateErrorNotifications(account, direction);
        }

        private boolean cancelled() {
            if (mDestroyed) {
                return true;
            }
            if (mCanceled) {
                finish();
                return true;
            }
            return false;
        }

        private void checkServerSettings(CheckDirection direction) throws MessagingException {
            switch (direction) {
                case INCOMING: {
                    checkIncoming();
                    break;
                }
                case OUTGOING: {
                    checkOutgoing();
                    break;
                }
            }
        }

        private void checkOutgoing() throws MessagingException {
            if (!(account.getRemoteStore() instanceof WebDavStore)) {
                publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
            }
            Transport transport = Transport.getInstance(K9.app, account);
            transport.close();
            transport.open();
            transport.close();
        }

        private void checkIncoming() throws MessagingException {
            Store store = account.getRemoteStore();
            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_authenticate);
            } else {
                publishProgress(R.string.account_setup_check_settings_check_incoming_msg);
            }
            store.checkSettings();

            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_fetch);
            }
            MessagingController.getInstance(getApplication()).listFoldersSynchronous(account, true, null);
            MessagingController.getInstance(getApplication())
                    .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            setMessage(values[0]);
        }
    }
}
